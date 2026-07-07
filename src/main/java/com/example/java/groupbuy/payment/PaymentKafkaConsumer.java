package com.example.java.groupbuy.payment;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.cart.repository.CartRepository;
import com.example.java.config.KafkaConfig;
import com.example.java.delivery.service.DeliveryService;
import com.example.java.groupbuy.service.GroupBuyService;
import com.example.java.member.entity.MemberCoupon;
import com.example.java.member.repository.MemberCouponRepository;
import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.Orders;
import com.example.java.orders.event.OrderPaidEvent;
import com.example.java.orders.event.OrderPaymentFailedEvent;
import com.example.java.orders.repository.OrderItemRepository;
import com.example.java.orders.repository.OrdersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 이벤트 Kafka Consumer.
 *
 * <p>OrderPaidEvent(결제 성공) 수신 시 다음 후처리를 수행한다:
 * <ol>
 *   <li>공구 주문: 공구 참여 확정 (GroupBuyService.confirmAfterPayment)</li>
 *   <li>일반 주문: 배송 생성 (DeliveryService.createDelivery)</li>
 *   <li>쿠폰 사용: 사용된 쿠폰을 사용완료 상태로 변경</li>
 *   <li>장바구니 삭제: 결제된 상품을 장바구니에서 제거 (CART 주문인 경우)</li>
 * </ol>
 *
 * <p>각 단계는 독립적으로 try-catch하여 하나의 실패가 다른 처리를 막지 않는다.
 * 단, 공구 참여 확정 실패는 예외를 전파하여 Kafka 재시도/DLQ가 작동하도록 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaConsumer {

    private final GroupBuyService groupBuyService;
    private final StringRedisTemplate redisTemplate;
    private final DeliveryService deliveryService;
    private final MemberCouponRepository memberCouponRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrdersRepository ordersRepository;

    @KafkaListener(topics = KafkaConfig.PAYMENT_SUCCESS_TOPIC, groupId = "groupbuy-group")
    @Transactional
    public void handlePaymentSuccess(OrderPaidEvent event) {
        log.info("Received Kafka OrderPaidEvent: orderSeq={}, isGroupBuy={}", event.orderSeq(), event.isGroupBuyOrder());

        String redisKey = "kafka:processed:" + event.eventId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            log.info("Duplicate event detected (already processed). Skipping eventId={}", event.eventId());
            return;
        }

        // 1. 공구 주문: 참여 확정 — 실패 시 Kafka 재시도/DLQ가 작동하도록 예외 전파
        if (event.participationSeqs() != null && !event.participationSeqs().isEmpty()) {
            for (Long seq : event.participationSeqs()) {
                groupBuyService.confirmAfterPayment(seq);
                log.info("Successfully confirmed group buy participation seq={}", seq);
            }
        }

        // 2. 일반 주문: 배송 생성 (공구 주문은 마감 확정 후 별도 흐름으로 배송 생성)
        if (!event.isGroupBuyOrder() && event.orderSeq() != null) {
            try {
                Orders order = ordersRepository.findById(event.orderSeq()).orElse(null);
                if (order != null) {
                    deliveryService.createDelivery(
                            order,
                            event.recipientName(),
                            event.recipientPhone(),
                            event.requestMemo(),
                            "B2C"
                    );
                    log.info("Delivery created for orderSeq={}", event.orderSeq());
                }
            } catch (Exception e) {
                log.error("배송 생성 실패 orderSeq={}: {}", event.orderSeq(), e.getMessage(), e);
            }
        }

        // 3. 쿠폰 사용 처리
        if (event.memberCouponSeq() != null) {
            try {
                MemberCoupon memberCoupon = memberCouponRepository.findById(event.memberCouponSeq()).orElse(null);
                if (memberCoupon != null && memberCoupon.getStatus() != null && memberCoupon.getStatus() == 0) {
                    memberCoupon.use();
                    log.info("Coupon used: memberCouponSeq={}", event.memberCouponSeq());
                }
            } catch (Exception e) {
                log.error("쿠폰 사용 처리 실패 memberCouponSeq={}: {}", event.memberCouponSeq(), e.getMessage(), e);
            }
        }

        // 4. 장바구니 삭제 (CART 주문인 경우만 — DIRECT 바로구매는 장바구니 없음)
        if (event.orderSeq() != null && event.memberSeq() != null
                && !"DIRECT".equals(event.orderSource())) {
            try {
                List<Long> orderedOptionsSeqList = orderItemRepository.findByOrderSeq(event.orderSeq())
                        .stream()
                        .map(OrderItem::getOptionsSeq)
                        .distinct()
                        .toList();
                if (!orderedOptionsSeqList.isEmpty()) {
                    cartRepository.deleteByMember_SeqAndOptions_SeqIn(event.memberSeq(), orderedOptionsSeqList);
                    log.info("Cart items deleted for memberSeq={}, orderSeq={}", event.memberSeq(), event.orderSeq());
                }
            } catch (Exception e) {
                log.error("장바구니 삭제 실패 orderSeq={}: {}", event.orderSeq(), e.getMessage(), e);
            }
        }

        // 성공적으로 처리가 완료되면 Redis에 처리 마킹 (24시간 유효)
        redisTemplate.opsForValue().set(redisKey, "true", java.time.Duration.ofHours(24));
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_FAILED_TOPIC, groupId = "groupbuy-group")
    public void handlePaymentFailed(OrderPaymentFailedEvent event) {
        log.info("Received Kafka OrderPaymentFailedEvent: {}", event);

        String redisKey = "kafka:processed:" + event.eventId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            log.info("Duplicate event detected (already processed). Skipping eventId={}", event.eventId());
            return;
        }

        if (event.participationSeqs() != null) {
            for (Long seq : event.participationSeqs()) {
                // 예외가 발생하면 밖으로 전파되도록 하여 Kafka 에러 핸들러(재시도 및 DLQ)가 가동되도록 함
                groupBuyService.cancelPendingPayment(seq);
                log.info("Successfully cancelled pending group buy participation seq={}", seq);
            }
        }

        // 성공적으로 처리가 완료되면 Redis에 처리 마킹 (24시간 유효)
        redisTemplate.opsForValue().set(redisKey, "true", java.time.Duration.ofHours(24));
    }

    // DLQ 모니터링 리스너
    @KafkaListener(topics = KafkaConfig.PAYMENT_SUCCESS_DLQ, groupId = "groupbuy-dlq-group")
    public void handlePaymentSuccessDlq(OrderPaidEvent event) {
        log.error("Received message in payment-success-topic.DLQ: {}", event);
        // 실무에서는 알림 발송, 장애 전파, 또는 DB 로그 적재 등을 수행
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_FAILED_DLQ, groupId = "groupbuy-dlq-group")
    public void handlePaymentFailedDlq(OrderPaymentFailedEvent event) {
        log.error("Received message in payment-failed-topic.DLQ: {}", event);
        // 실무에서는 알림 발송, 장애 전파, 또는 DB 로그 적재 등을 수행
    }
}
