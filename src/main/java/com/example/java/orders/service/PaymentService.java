package com.example.java.orders.service;

import com.example.java.delivery.entity.Delivery;
import com.example.java.delivery.repository.DeliveryRepository;
import com.example.java.delivery.service.DeliveryService;
import com.example.java.member.entity.MemberCoupon;
import com.example.java.member.repository.MemberCouponRepository;
import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.Orders;
import com.example.java.orders.entity.Payment;
import com.example.java.orders.entity.Refund;
import com.example.java.orders.repository.OrderItemRepository;
import com.example.java.orders.repository.OrdersRepository;
import com.example.java.orders.repository.PaymentRepository;
import com.example.java.delivery.entity.DeliveryCompany;
import com.example.java.orders.entity.RefundReason;
import com.example.java.orders.entity.ReturnRequest;
import com.example.java.orders.entity.Returns;
import com.example.java.orders.repository.RefundReasonRepository;
import com.example.java.orders.repository.ReturnRequestRepository;
import com.example.java.orders.repository.ReturnsRepository;
import com.example.java.outbox.OutboxEvent;
import com.example.java.outbox.OutboxEventRepository;
import com.example.java.orders.repository.RefundRepository;
import com.example.java.orders.event.OrderPaidEvent;
import com.example.java.orders.event.OrderPaymentFailedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.java.product.entity.Options;
import com.example.java.product.repository.OptionsRepository;
import com.example.java.product.entity.Seller;
import com.example.java.product.repository.SellerRepository;
import com.example.java.stockhistory.entity.StockHistory;
import com.example.java.stockhistory.enums.StockHistorySourceType;
import com.example.java.stockhistory.enums.StockHistoryType;
import com.example.java.stockhistory.repository.StockHistoryRepository;
import com.example.java.orders.repository.OrdersQueryRepository;
import jakarta.persistence.EntityManager;

import java.util.LinkedHashMap;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final RefundRepository refundRepository;
    private final ObjectMapper objectMapper;
    private final OptionsRepository optionsRepository;
    private final SellerRepository sellerRepository;
    private final DeliveryService deliveryService;
    private final StockHistoryRepository stockHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RefundReasonRepository refundReasonRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnsRepository returnsRepository;
    private final com.example.java.common.util.SnowflakeIdGenerator snowflakeIdGenerator;
    private final OrdersQueryRepository ordersQueryRepository;
    private final com.example.java.product.service.OptionsService optionsService;
    private final EntityManager entityManager;
    private final OutboxEventRepository outboxEventRepository;
    
    @Value("${toss.secret-key}")
    private String tossSecretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_PAYMENT_URL = "https://api.tosspayments.com/v1/payments";

    /**
     * 토스 결제 성공 후 호출되는 최종 승인 처리.
     */
    @Transactional
    public void confirmPayment(String paymentKey,
                               String orderId,
                               Integer amount) {

        Orders order = ordersRepository.findByOrderUid(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderId=" + orderId));

        /*
            이미 결제 완료된 주문이면 중복 처리하지 않는다.
            토스 성공 URL 새로고침, 브라우저 재요청 등에 대비한 처리다.
         */
        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 2) {
            return;
        }

        /*
            서버 DB에 저장된 최종 결제금액과 토스에서 돌아온 amount를 반드시 비교한다.
            화면 hidden amount 값은 사용자가 조작할 수 있으므로 신뢰하면 안 된다.
         */
        if (order.getFinalPrice() == null || !order.getFinalPrice().equals(amount)) {
            order.setPaymentStatus(3);
            ordersRepository.save(order);

            throw new IllegalArgumentException(
                    "결제 금액이 일치하지 않습니다. DB finalPrice="
                            + order.getFinalPrice()
                            + ", toss amount="
                            + amount
            );
        }

        /*
            같은 paymentKey로 이미 저장된 결제내역이 있으면 중복 저장하지 않는다.
         */
        if (paymentRepository.findByExternalPaymentId(paymentKey).isPresent()) {
            return;
        }

        /*
            결제 승인 전에 주문상품 기준으로 재고를 잠그고 차감한다.

            이 위치에서 처리하는 이유:
            - 재고가 부족하면 Toss confirm을 호출하지 않는다.
            - 같은 상품을 동시에 결제해도 options row를 잠가서 중복 차감을 방지한다.
            - Toss confirm 실패 시 현재 트랜잭션이 롤백되므로 재고 차감도 롤백된다.
         */
        List<OrderItem> orderItems = orderItemRepository.findByOrderSeq(order.getSeq());

        if (orderItems.isEmpty()) {
            throw new IllegalStateException("주문상품 정보가 없습니다. orderSeq=" + order.getSeq());
        }

        /*
            공구 주문(order_item.participation_seq != null)은 재고를 발주 입고로 충당하므로
            결제 시점에 일반 재고(options.stock)를 깎지 않는다. 일반 주문 상품만 차감 대상.
         */
        List<OrderItem> stockTargets = orderItems.stream()
                .filter(item -> item.getParticipationSeq() == null)
                .toList();

        if (!stockTargets.isEmpty()) {
            decreaseStockForOrderItems(stockTargets, order.getSeq());
        }

        JsonNode tossResponse = requestTossConfirm(paymentKey, orderId, amount);

        String status = getText(tossResponse, "status");

        if (!"DONE".equals(status)) {
            order.setPaymentStatus(3);
            ordersRepository.save(order);

            throw new IllegalStateException("토스 결제 승인이 완료되지 않았습니다. status=" + status);
        }

        String method = getText(tossResponse, "method");
        String approvedAt = getText(tossResponse, "approvedAt");

        String receiptUrl = null;

        if (tossResponse.has("receipt")
                && tossResponse.get("receipt") != null
                && tossResponse.get("receipt").has("url")) {
            receiptUrl = tossResponse.get("receipt").get("url").asText();
        }

        LocalDateTime approvedDateTime = parseTossDateTime(approvedAt);

        Payment payment = Payment.builder()
                .orderSeq(order.getSeq())
                .paymentUid(createPaymentUid())
                .externalPaymentId(paymentKey)
                .pgTid(paymentKey)
                .paymentMethod(mapPaymentMethod(method))
                .pgProvider("TOSS")
                .status(2)
                .amount(amount)
                .requestDate(LocalDateTime.now())
                .payDate(approvedDateTime)
                .receiptUrl(receiptUrl)
                .failReason(null)
                .updateDate(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        order.setOrderStatus(2);
        order.setPaymentStatus(2);
        order.setOrderDate(approvedDateTime);
        order.setRemainPrice(amount);

        ordersRepository.save(order);

        /*
            결제 완료 후 OrderPaidEvent를 Kafka로 발행한다.
            배송 생성, 쿠폰 사용 처리, 장바구니 삭제는 모두 Kafka Consumer(PaymentKafkaConsumer)가 처리한다.
            공구 주문이면 Consumer에서 참여 확정(confirmAfterPayment)도 함께 처리한다.
         */
        boolean isGroupBuyOrder = orderItems.stream()
                .anyMatch(item -> item.getParticipationSeq() != null);

        List<Long> participationSeqs = orderItems.stream()
                .map(OrderItem::getParticipationSeq)
                .filter(seq -> seq != null)
                .toList();

        // field 파싱: CART|recipientName|phone|memo 형식
        String field = order.getField();
        String recipientName = "고객";
        String recipientPhone = "010-0000-0000";
        String requestMemo = "";
        String orderSource = null;
        if (field != null && field.contains("|")) {
            String[] parts = field.split("\\|", -1);
            orderSource = parts[0]; // "CART" 또는 "DIRECT"
            if (parts.length >= 2 && !parts[1].isBlank()) {
                recipientName = parts[1];
            }
            if (parts.length >= 3 && !parts[2].isBlank()) {
                recipientPhone = parts[2];
            }
            if (parts.length >= 4) {
                requestMemo = parts[3];
            }
        }

//        eventPublisher.publishEvent(new OrderPaidEvent(
//                java.util.UUID.randomUUID().toString(),
//                order.getSeq(),
//                order.getMemberSeq(),
//                order.getMemberCouponSeq(),
//                isGroupBuyOrder,
//                orderSource,
//                recipientName,
//                recipientPhone,
//                requestMemo,
//                participationSeqs
//        ));
        

     // 💡 변경 코드: Outbox 테이블에 저장 (동일한 DB 트랜잭션 내에 묶임)
     OrderPaidEvent eventPayload = new OrderPaidEvent(
             java.util.UUID.randomUUID().toString(),
             order.getSeq(),
             order.getMemberSeq(),
             order.getMemberCouponSeq(),
             isGroupBuyOrder,
             orderSource,
             recipientName,
             recipientPhone,
             requestMemo,
             participationSeqs
     );

     // ObjectMapper를 주입받아 사용 (JSON 파싱용)
     String jsonPayload;
	try {
		jsonPayload = objectMapper.writeValueAsString(eventPayload);
	} catch (JsonProcessingException e) {
		log.error("치명적 오류: Event 객체를 JSON으로 변환하는 데 실패했습니다. orderSeq: {}", order.getSeq(), e);
	    throw new IllegalStateException("결제 완료 이벤트 생성 중 서버 오류가 발생했습니다.", e);
	}

     OutboxEvent outbox = OutboxEvent.builder()
             .eventId(UUID.randomUUID().toString())
             .eventType("OrderPaidEvent")
             .aggregateType("ORDER")
             .aggregateId(order.getSeq())
             .payload(jsonPayload)
             .status("READY")
             .createdAt(LocalDateTime.now())
             .build();

     outboxEventRepository.save(outbox);
        
    }

    /**
     * 기존 전체 주문 취소용 메서드.
     *
     * 내부적으로 아직 취소되지 않은 모든 order_item을 선택해서
     * cancelOrderItems()를 호출한다.
     */
    @Transactional
    public void cancelPayment(Long orderSeq,
                              Long memberSeq) {

        if (orderSeq == null) {
            throw new IllegalArgumentException("주문번호가 없습니다.");
        }

        Orders order = ordersRepository.findById(orderSeq)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderSeq=" + orderSeq));

        List<Long> orderItemSeqList = orderItemRepository.findByOrderSeq(order.getSeq())
                .stream()
                .filter(item -> item.getItemStatus() == null || item.getItemStatus() != 6)
                .map(OrderItem::getSeq)
                .toList();

        if (orderItemSeqList.isEmpty()) {
            throw new IllegalStateException("취소할 수 있는 주문상품이 없습니다.");
        }

        cancelOrderItems(orderSeq, memberSeq, orderItemSeqList, "고객 주문 취소");
    }

    /**
     * 선택한 주문상품만 부분취소한다.
     *
     * 현재 기준:
     * - 상품 라인 단위 부분취소
     * - 수량 일부 취소는 아직 지원하지 않음
     * - 선택한 order_item 전체 수량을 취소함
     *
     * 취소 가능 조건:
     * 1. 로그인 회원 본인의 주문
     * 2. 결제완료 또는 부분환불 상태의 주문
     * 3. 배송 상태가 READY 또는 배송 row 미생성 상태
     * 4. 아직 취소되지 않은 order_item
     */
    @Transactional
    public void cancelOrderItems(Long orderSeq,
                                 Long memberSeq,
                                 List<Long> orderItemSeqList,
                                 String cancelReason) {

        if (orderSeq == null) {
            throw new IllegalArgumentException("주문번호가 없습니다.");
        }

        if (memberSeq == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (orderItemSeqList == null || orderItemSeqList.isEmpty()) {
            throw new IllegalArgumentException("취소할 상품을 선택해야 합니다.");
        }

        String reason = isBlank(cancelReason) ? "고객 부분 주문 취소" : cancelReason;

        Orders order = ordersRepository.findBySeqForUpdate(orderSeq)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderSeq=" + orderSeq));

        entityManager.refresh(order); // 대기 중 다른 트랜잭션에 의해 변경된 최신 상태를 DB로부터 동기화

        if (!order.getMemberSeq().equals(memberSeq)) {
            throw new IllegalArgumentException("본인의 주문만 취소할 수 있습니다.");
        }

        if (order.getOrderStatus() != null && order.getOrderStatus() == 9) {
            throw new IllegalStateException("이미 전체 취소된 주문입니다.");
        }

        /*
            payment_status
            2: 결제완료
            4: 부분환불
            6: 결제취소
         */
        if (order.getPaymentStatus() == null
                || !(order.getPaymentStatus() == 2 || order.getPaymentStatus() == 4)) {
            throw new IllegalStateException("결제완료 또는 부분환불 상태의 주문만 취소할 수 있습니다.");
        }

        Payment payment = paymentRepository.findTopByOrderSeqAndStatusOrderBySeqDesc(order.getSeq(), 2)
                .orElseThrow(() -> new IllegalStateException("결제완료 정보를 찾을 수 없습니다. orderSeq=" + order.getSeq()));

        if (payment.getExternalPaymentId() == null || payment.getExternalPaymentId().isBlank()) {
            throw new IllegalStateException("토스 paymentKey가 없어 결제를 취소할 수 없습니다.");
        }

        Set<Long> distinctOrderItemSeqSet = new LinkedHashSet<>(orderItemSeqList);

        List<OrderItem> allOrderItems = orderItemRepository.findByOrderSeq(order.getSeq());

        List<OrderItem> cancelItems = allOrderItems.stream()
                .filter(item -> distinctOrderItemSeqSet.contains(item.getSeq()))
                .toList();

        if (cancelItems.size() != distinctOrderItemSeqSet.size()) {
            throw new IllegalArgumentException("선택한 상품 중 해당 주문에 포함되지 않은 상품이 있습니다.");
        }

        for (OrderItem item : cancelItems) {
            if (item.getItemStatus() != null && item.getItemStatus() == 6) {
                throw new IllegalStateException("이미 취소된 상품이 포함되어 있습니다. orderItemSeq=" + item.getSeq());
            }

            int quantity = nullToZero(item.getQuantity());
            int refundQuantity = nullToZero(item.getRefundQuantity());

            if (quantity <= 0) {
                throw new IllegalStateException("취소할 수량이 없는 상품이 포함되어 있습니다. orderItemSeq=" + item.getSeq());
            }

            if (refundQuantity >= quantity) {
                throw new IllegalStateException("이미 전량 환불된 상품이 포함되어 있습니다. orderItemSeq=" + item.getSeq());
            }
        }

        /*
            배송대기 상태에서만 취소 가능.
            취소하려는 상품들의 택배사(DeliveryCompany)명을 기준으로, 
            해당 주문의 연계된 배송 건 중 동일한 택배사를 가진 배송 상태가 READY인지 검증한다.
         */
        List<String> cancelItemDeliveryCompanies = cancelItems.stream()
                .map(item -> {
                    Options opt = optionsRepository.findById(item.getOptionsSeq()).orElse(null);
                    if (opt != null && opt.getProduct() != null && opt.getProduct().getSellerSeq() != null) {
                        Seller sel = sellerRepository.findById(opt.getProduct().getSellerSeq()).orElse(null);
                        if (sel != null && sel.getDeliveryCompany() != null) {
                            return sel.getDeliveryCompany().getName();
                        }
                    }
                    return "배송 준비중";
                })
                .distinct()
                .toList();

        List<Delivery> deliveries = deliveryRepository.findByOrders_Seq(order.getSeq());
        if (deliveries != null && !deliveries.isEmpty()) {
            for (Delivery d : deliveries) {
                String dCompanyName = (d.getDeliveryCompany() != null) ? d.getDeliveryCompany().getName() : "배송 준비중";
                if (cancelItemDeliveryCompanies.contains(dCompanyName)) {
                    if (!"READY".equals(d.getStatus())) {
                        throw new IllegalStateException("배송이 시작되어서 주문취소가 불가능합니다.");
                    }
                }
            }
        }

        boolean allItemsCanceled = isAllItemsCanceled(allOrderItems, distinctOrderItemSeqSet);
        
        int cancelAmount = calculateOrderCancelAmount(
                order,
                allOrderItems,
                cancelItems,
                distinctOrderItemSeqSet,
                allItemsCanceled
        );

        if (cancelAmount <= 0) {
            throw new IllegalStateException("취소할 금액이 없습니다.");
        }

        int remainPrice = getRemainPrice(order);

        if (cancelAmount > remainPrice) {
            throw new IllegalStateException(
                    "취소 금액이 남은 결제금액보다 큽니다. cancelAmount="
                            + cancelAmount
                            + ", remainPrice="
                            + remainPrice
            );
        }

        /*
            Toss 부분취소 API 호출.
            cancelAmount를 보내므로 선택 상품 금액만 취소된다.
         */
        try {
            requestTossCancel(payment.getExternalPaymentId(), reason, cancelAmount);
        } catch (ResponseStatusException e) {
            String errorMsg = e.getReason();
            if (errorMsg != null && (errorMsg.contains("ALREADY_CANCELED_PAYMENT") || errorMsg.contains("이미 취소된 결제"))) {
                log.warn("[Toss API] Payment is already canceled. Proceeding with DB update for idempotency: {}", errorMsg);
            } else {
                throw e;
            }
        }

        /*
            결제 취소 성공 후 취소한 주문상품 수량만큼 재고를 복구한다.
            단, 공구 주문(participation_seq != null)은 결제 때 일반 재고를 깎지 않았으므로(발주로 충당)
            환불 때도 재고를 복구하지 않는다. 일반 주문상품만 복구 대상.
         */
        List<OrderItem> stockRestoreTargets = cancelItems.stream()
                .filter(item -> item.getParticipationSeq() == null)
                .toList();

        if (!stockRestoreTargets.isEmpty()) {
            increaseStockForOrderItems(stockRestoreTargets, order.getSeq());
        }

        LocalDateTime now = LocalDateTime.now();

        /*
            refund 테이블에 order_item별 환불 기록 저장.
         */
        List<Refund> refunds = cancelItems.stream()
                .map(item -> createRefundEntity(item, payment, now))
                .toList();

        refundRepository.saveAll(refunds);

        /*
            order_item 상태 갱신.
            선택한 상품 라인 전체 수량을 취소 처리한다.
         */
        for (OrderItem item : cancelItems) {
            item.setRefundQuantity(item.getQuantity());
            item.setRefundPrice(item.getSubTotalPrice());
            item.setItemStatus(6);
        }

        orderItemRepository.saveAll(cancelItems);

        /*
            orders 금액 및 상태 갱신.
         */
        int newTotalRefundPrice = nullToZero(order.getTotalRefundPrice()) + cancelAmount;
        int newRemainPrice = nullToZero(order.getFinalPrice()) - newTotalRefundPrice;

        if (newRemainPrice < 0) {
            newRemainPrice = 0;
        }

        order.setTotalRefundPrice(newTotalRefundPrice);
        order.setRemainPrice(newRemainPrice);

		

        if (allItemsCanceled) {
            /*
                모든 주문상품이 취소된 경우.
             */
            order.setOrderStatus(9);
            order.setPaymentStatus(6);

            payment.setStatus(4);
            payment.setFailReason(reason);

            /*
                전체 취소일 때만 쿠폰 복구.
                부분취소에서는 남은 상품에 쿠폰이 계속 적용된 상태로 유지한다.
             */
            restoreCouponIfExists(order);

            /*
                배송 row가 이미 있다면 실패 처리.
             */
            deliveryService.cancelAllDeliveriesForOrder(order.getSeq());

        } else {
            /*
                일부 상품만 취소된 경우.
                order_status = 7 : 부분환불 또는 부분취소
                payment_status = 4 : 부분환불
             */
            order.setOrderStatus(7);
            order.setPaymentStatus(4);

            /*
                취소된 특정 택배사 묶음의 배송 레코드를 FAILED 상태로 변경
             */
            deliveryService.cancelDeliveriesForOrderAndCompanies(order.getSeq(), cancelItemDeliveryCompanies);
        }

        payment.setUpdateDate(now);

        ordersRepository.save(order);
        paymentRepository.save(payment);
    }

    /**
     * 결제 실패 URL로 돌아온 경우 주문과 결제 실패 기록을 남긴다.
     */
    @Transactional
    public void markPaymentFail(String orderId,
                                String code,
                                String message) {

        if (orderId == null || orderId.isBlank()) {
            return;
        }

        Orders order = ordersRepository.findByOrderUid(orderId)
                .orElse(null);

        if (order == null) {
            return;
        }

        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 2) {
            return;
        }

        order.setPaymentStatus(3);
        ordersRepository.save(order);

        String failReason = "[" + nullToEmpty(code) + "] " + nullToEmpty(message);

        Payment payment = Payment.builder()
                .orderSeq(order.getSeq())
                .paymentUid(createPaymentUid())
                .externalPaymentId(null)
                .pgTid(null)
                .paymentMethod(0)
                .pgProvider("TOSS")
                .status(3)
                .amount(order.getFinalPrice())
                .requestDate(LocalDateTime.now())
                .payDate(null)
                .receiptUrl(null)
                .failReason(failReason)
                .updateDate(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        /*
            공구 주문이면 '결제 실패(취소)' 이벤트를 발행한다. 결제 도메인은 공구를 모른 채 방송만 하고,
            groupbuy 리스너가 이를 받아 결제대기 participation을 CANCELLED로 돌리고 점유를 반납한다.
            (사용자가 토스 결제창에서 명시적으로 취소한 경우 — 자리를 즉시 반납해 매진 대기자에게 양보)
            OrderPaidEvent(결제 완료)와 대칭. @EventListener 동기라 이 트랜잭션 안에서 함께 처리된다.
         */
        List<Long> participationSeqs = orderItemRepository.findByOrderSeq(order.getSeq()).stream()
                .map(OrderItem::getParticipationSeq)
                .filter(seq -> seq != null)
                .toList();

        if (!participationSeqs.isEmpty()) {
            eventPublisher.publishEvent(new OrderPaymentFailedEvent(participationSeqs));
        }
    }

    private void validateDeliveryReady(Long orderSeq) {
        List<Delivery> deliveries = deliveryRepository.findByOrders_Seq(orderSeq);

        if (deliveries == null || deliveries.isEmpty()) {
            return;
        }

        boolean allReady = deliveries.stream()
                .allMatch(delivery -> "READY".equals(delivery.getStatus()));

        if (!allReady) {
            throw new IllegalStateException("배송대기 상태의 주문만 취소할 수 있습니다.");
        }
    }

    private int calculateCancelAmount(List<OrderItem> cancelItems) {
        return cancelItems.stream()
                .mapToInt(item -> nullToZero(item.getSubTotalPrice()) - nullToZero(item.getRefundPrice()))
                .sum();
    }
    
    /**
     * 주문취소 환불금액 계산.
     *
     * 주문취소는 배송 시작 전 취소이므로,
     * 취소 대상 택배사 묶음이 완전히 취소되는 경우 배송비도 환불한다.
     *
     * 반품은 이 메서드를 쓰면 안 된다.
     * 반품은 배송 완료 후 환불이므로 배송비를 제외하고 calculateCancelAmount()만 사용한다.
     */
    private int calculateOrderCancelAmount(Orders order,
                                           List<OrderItem> allOrderItems,
                                           List<OrderItem> cancelItems,
                                           Set<Long> currentCancelItemSeqSet,
                                           boolean allItemsCanceled) {

        /*
            전체 주문취소라면 remainPrice 전액 환불.
            remainPrice에는 실제 결제된 남은 금액이 들어 있으므로
            배송비, 쿠폰 배분 오차, 이전 부분환불까지 가장 안전하게 반영된다.
         */
        if (allItemsCanceled) {
            return getRemainPrice(order);
        }

        int productCancelAmount = calculateCancelAmount(cancelItems);

        /*
            멤버십 회원은 결제 시 배송비가 0원이므로
            부분 주문취소에서도 배송비를 추가 환불하면 안 된다.
         */
        boolean usableMembership = ordersQueryRepository.existsUsableMembership(order.getMemberSeq());

        if (usableMembership) {
            return productCancelAmount;
        }

        int deliveryFeeRefundAmount = calculateCancelableDeliveryFee(
                allOrderItems,
                cancelItems,
                currentCancelItemSeqSet
        );

        return productCancelAmount + deliveryFeeRefundAmount;
    }

    private Refund createRefundEntity(OrderItem item,
                                      Payment payment,
                                      LocalDateTime now) {

        int quantity = nullToZero(item.getQuantity());
        int alreadyRefundQuantity = nullToZero(item.getRefundQuantity());
        int cancelQuantity = quantity - alreadyRefundQuantity;

        if (cancelQuantity <= 0) {
            cancelQuantity = quantity;
        }

        int refundPrice = nullToZero(item.getSubTotalPrice()) - nullToZero(item.getRefundPrice());

        if (refundPrice < 0) {
            refundPrice = 0;
        }

        /*
            refund_product_price는 상품 원가 기준 환불 대상 금액.
            original_price가 1개 단가이므로 취소 수량을 곱한다.
         */
        int refundProductPrice = nullToZero(item.getOriginalPrice()) * cancelQuantity;

        return Refund.builder()
                .orderItemSeq(item.getSeq())
                .paymentSeq(payment.getSeq())
                .returnRequest(null)
                .returnsSeq(null)
                .refundUid(createRefundUid(item.getSeq()))
                .refundQuantity(cancelQuantity)
                .refundProductPrice(refundProductPrice)
                .refundHotdeal(nullToZero(item.getHotdealDiscount()))
                .refundCoupon(nullToZero(item.getCouponDiscount()))
                .refundParticipation(nullToZero(item.getParticipationDiscount()))
                .refundPrice(refundPrice)
                .status(2)
                .requestDate(now)
                .completeDate(now)
                .updateDate(now)
                .build();
    }

    private boolean isAllItemsCanceled(List<OrderItem> allOrderItems,
                                       Set<Long> currentCancelItemSeqSet) {

        for (OrderItem item : allOrderItems) {
            boolean alreadyCanceled = item.getItemStatus() != null && item.getItemStatus() == 6;
            boolean nowCanceled = currentCancelItemSeqSet.contains(item.getSeq());

            if (!alreadyCanceled && !nowCanceled) {
                return false;
            }
        }

        return true;
    }

    private int getRemainPrice(Orders order) {
        if (order.getRemainPrice() != null) {
            return order.getRemainPrice();
        }

        return nullToZero(order.getFinalPrice()) - nullToZero(order.getTotalRefundPrice());
    }

    private void restoreCouponIfExists(Orders order) {
        if (order.getMemberCouponSeq() == null) {
            return;
        }

        memberCouponRepository.findById(order.getMemberCouponSeq())
                .ifPresent(memberCoupon -> memberCoupon.updateStatus(0));
    }


    private JsonNode requestTossConfirm(String paymentKey,
                                        String orderId,
                                        Integer amount) {

        if (paymentKey != null && paymentKey.startsWith("mock_")) {
            log.info("[MOCK CONFIRM] Bypassing Toss API confirm for mock key: {}", paymentKey);
            com.fasterxml.jackson.databind.node.ObjectNode mockNode = objectMapper.createObjectNode();
            mockNode.put("status", "DONE");
            mockNode.put("method", "계좌이체");
            mockNode.put("approvedAt", java.time.OffsetDateTime.now().toString());
            return mockNode;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("paymentKey", paymentKey);
            body.put("orderId", orderId);
            body.put("amount", amount);

            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOSS_CONFIRM_URL))
                    .header("Authorization", createBasicAuthorizationHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode responseBody = objectMapper.readTree(response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorMessage = responseBody.has("message")
                        ? responseBody.get("message").asText()
                        : response.body();

                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(response.statusCode()),
                        errorMessage
                );
            }

            return responseBody;

        } catch (IOException e) {
            throw new IllegalStateException("토스 결제 승인 응답 처리 중 오류가 발생했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("토스 결제 승인 요청이 중단되었습니다.", e);
        }
    }

    /**
     * 토스 부분취소/전체취소 API 호출.
     *
     * cancelAmount를 보내면 부분취소,
     * 선택한 상품들이 주문 전체라면 결과적으로 전체취소가 된다.
     */
    private JsonNode requestTossCancel(String paymentKey,
                                       String cancelReason,
                                       Integer cancelAmount) {

        if (paymentKey != null && paymentKey.startsWith("mock_")) {
            log.info("[MOCK CANCEL] Bypassing Toss API cancel for mock key: {}", paymentKey);
            com.fasterxml.jackson.databind.node.ObjectNode mockNode = objectMapper.createObjectNode();
            mockNode.put("status", "CANCELED");
            return mockNode;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("cancelReason", cancelReason);
            body.put("cancelAmount", cancelAmount);

            String requestBody = objectMapper.writeValueAsString(body);

            String encodedPaymentKey = URLEncoder.encode(paymentKey, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOSS_PAYMENT_URL + "/" + encodedPaymentKey + "/cancel"))
                    .header("Authorization", createBasicAuthorizationHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode responseBody = objectMapper.readTree(response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorMessage = responseBody.has("message")
                        ? responseBody.get("message").asText()
                        : response.body();

                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(response.statusCode()),
                        errorMessage
                );
            }


            return responseBody;

        } catch (IOException e) {
            throw new IllegalStateException("토스 결제 취소 응답 처리 중 오류가 발생했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("토스 결제 취소 요청이 중단되었습니다.", e);
        }
    }

    private String createBasicAuthorizationHeader() {
        String value = tossSecretKey + ":";
        String encoded = Base64.getEncoder()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));

        return "Basic " + encoded;
    }

    private Integer mapPaymentMethod(String method) {
        if (method == null) {
            return 0;
        }

        return switch (method) {
            case "카드" -> 0;
            case "계좌이체" -> 1;
            case "가상계좌" -> 2;
            default -> 0;
        };
    }

    private LocalDateTime parseTossDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }

        return OffsetDateTime.parse(value).toLocalDateTime();
    }

    private String getText(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }

        return node.get(fieldName).asText();
    }

    private String createPaymentUid() {
        return "PAY-" + System.currentTimeMillis();
    }

    private String createRefundUid(Long orderItemSeq) {
        return "REF-" + System.currentTimeMillis() + "-" + orderItemSeq;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
    
    /**
     * 결제 완료 시 주문상품 수량만큼 옵션 재고를 감소시킨다.
     */
    private void decreaseStockForOrderItems(List<OrderItem> orderItems, Long orderSeq) {
        LinkedHashMap<Long, Integer> quantityByOptionsSeq = aggregateQuantityByOptionsSeq(orderItems);
        java.util.Set<Long> productSeqsToUpdate = new java.util.HashSet<>();

        for (Map.Entry<Long, Integer> entry : quantityByOptionsSeq.entrySet()) {
            Long optionsSeq = entry.getKey();
            int decreaseQuantity = entry.getValue();

            Options options = optionsRepository.findBySeqForUpdate(optionsSeq)
                    .orElseThrow(() -> new IllegalStateException("옵션 정보를 찾을 수 없습니다. optionsSeq=" + optionsSeq));

            int beforeStock = nullToZero(options.getStock());

            if (beforeStock < decreaseQuantity) {
                throw new IllegalStateException(
                        "재고가 부족합니다. optionsSeq="
                                + optionsSeq
                                + ", 현재재고="
                                + beforeStock
                                + ", 구매수량="
                                + decreaseQuantity
                );
            }

            options.decreaseStock(decreaseQuantity);
            optionsRepository.saveAndFlush(options);

            if (options.getProduct() != null) {
                productSeqsToUpdate.add(options.getProduct().getSeq());
            }

            int afterStock = nullToZero(options.getStock());

            saveStockHistory(
                    options,
                    StockHistoryType.OUT,
                    "주문 결제 재고 차감",
                    decreaseQuantity,
                    beforeStock,
                    afterStock,
                    orderSeq,
                    StockHistorySourceType.주문
            );
        }

        for (Long productSeq : productSeqsToUpdate) {
            optionsService.updateProductSaleStatus(productSeq);
        }
    }

    /**
     * 주문취소 또는 부분취소 시 취소한 주문상품 수량만큼 옵션 재고를 복구한다.
     */
    private void increaseStockForOrderItems(List<OrderItem> orderItems, Long orderSeq) {
        LinkedHashMap<Long, Integer> quantityByOptionsSeq = aggregateQuantityByOptionsSeq(orderItems);
        java.util.Set<Long> productSeqsToUpdate = new java.util.HashSet<>();

        for (Map.Entry<Long, Integer> entry : quantityByOptionsSeq.entrySet()) {
            Long optionsSeq = entry.getKey();
            int increaseQuantity = entry.getValue();

            Options options = optionsRepository.findBySeqForUpdate(optionsSeq)
                    .orElseThrow(() -> new IllegalStateException("옵션 정보를 찾을 수 없습니다. optionsSeq=" + optionsSeq));

            int beforeStock = nullToZero(options.getStock());

            options.increaseStock(increaseQuantity);
            optionsRepository.saveAndFlush(options);

            if (options.getProduct() != null) {
                productSeqsToUpdate.add(options.getProduct().getSeq());
            }

            int afterStock = nullToZero(options.getStock());

            saveStockHistory(
                    options,
                    StockHistoryType.IN,
                    "주문 취소 재고 복구",
                    increaseQuantity,
                    beforeStock,
                    afterStock,
                    orderSeq,
                    StockHistorySourceType.주문
            );
        }

        for (Long productSeq : productSeqsToUpdate) {
            optionsService.updateProductSaleStatus(productSeq);
        }
    }

    /**
     * 같은 옵션이 주문상품에 여러 번 들어간 경우를 대비해서 optionsSeq별 수량을 합산한다.
     */
    private LinkedHashMap<Long, Integer> aggregateQuantityByOptionsSeq(List<OrderItem> orderItems) {
        LinkedHashMap<Long, Integer> quantityByOptionsSeq = new LinkedHashMap<>();

        for (OrderItem item : orderItems) {
            if (item.getOptionsSeq() == null) {
                throw new IllegalStateException("주문상품의 옵션 번호가 없습니다. orderItemSeq=" + item.getSeq());
            }

            int quantity = nullToZero(item.getQuantity());

            if (quantity <= 0) {
                throw new IllegalStateException("주문상품 수량이 올바르지 않습니다. orderItemSeq=" + item.getSeq());
            }

            quantityByOptionsSeq.merge(
                    item.getOptionsSeq(),
                    quantity,
                    Integer::sum
            );
        }

        return quantityByOptionsSeq;
    }

    /**
     * 재고 변동 이력을 stock_history 테이블에 저장한다.
     */
    private void saveStockHistory(Options options,
                                  StockHistoryType type,
                                  String reason,
                                  int quantity,
                                  int beforeStock,
                                  int afterStock,
                                  Long sourceSeq,
                                  StockHistorySourceType sourceType) {

        StockHistory stockHistory = StockHistory.builder()
                .options(options)
                .type(type)
                .reason(reason)
                .quantity(quantity)
                .beforeStock(beforeStock)
                .afterStock(afterStock)
                .sourceSeq(sourceSeq)
                .sourceType(sourceType)
                .createdAt(LocalDateTime.now())
                .build();

        stockHistoryRepository.save(stockHistory);
    }

    /**
     * 선택한 주문상품들을 반품 요청 상태(7)로 변경하고 즉시 환불 처리 및 반품 배송 접수한다.
     */
    @Transactional
    public void requestReturnItems(Long orderSeq,
                                  Long memberSeq,
                                  List<Long> orderItemSeqList,
                                  String reason) {
        if (orderSeq == null) {
            throw new IllegalArgumentException("주문번호가 없습니다.");
        }
        if (memberSeq == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (orderItemSeqList == null || orderItemSeqList.isEmpty()) {
            throw new IllegalArgumentException("반품할 상품을 선택해야 합니다.");
        }

        Orders order = ordersRepository.findById(orderSeq)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderSeq=" + orderSeq));

        if (!order.getMemberSeq().equals(memberSeq)) {
            throw new IllegalArgumentException("본인의 주문만 반품 신청할 수 있습니다.");
        }

        List<OrderItem> allOrderItems = orderItemRepository.findByOrderSeq(order.getSeq());
        Set<Long> targetSeqs = new LinkedHashSet<>(orderItemSeqList);

        List<OrderItem> targetItems = allOrderItems.stream()
                .filter(item -> targetSeqs.contains(item.getSeq()))
                .toList();

        if (targetItems.size() != targetSeqs.size()) {
            throw new IllegalArgumentException("선택한 상품 중 해당 주문에 포함되지 않은 상품이 있습니다.");
        }

        // 1. 검증: 이미 취소되거나 반품 진행 중인 상품인지 확인
        for (OrderItem item : targetItems) {
            if (item.getItemStatus() != null) {
                if (item.getItemStatus() == 6) {
                    throw new IllegalStateException("이미 취소된 상품이 포함되어 있습니다. 상품명: " + item.getProductName());
                }
                if (item.getItemStatus() >= 7) {
                    throw new IllegalStateException("이미 반품이 신청되었거나 완료된 상품입니다. 상품명: " + item.getProductName());
                }
            }
        }

        // 2. 백엔드 방어 로직: 해당 상품의 배송 상태가 실제로 완료(DELIVERED)인지 검증
        List<Delivery> deliveries = deliveryRepository.findByOrders_Seq(order.getSeq());
        for (OrderItem item : targetItems) {
            Options opt = optionsRepository.findById(item.getOptionsSeq())
                    .orElseThrow(() -> new IllegalArgumentException("상품 옵션 정보를 찾을 수 없습니다."));
            Seller sel = sellerRepository.findById(opt.getProduct().getSellerSeq())
                    .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
            DeliveryCompany company = sel.getDeliveryCompany();
            if (company == null) {
                throw new IllegalStateException("해당 상품의 배송 택배사 정보가 없습니다.");
            }

            // 판매자 택배사와 일치하는 배송 건 조회
            List<Delivery> itemDeliveries = deliveries.stream()
                    .filter(d -> d.getDeliveryCompany() != null && d.getDeliveryCompany().getSeq().equals(company.getSeq()))
                    .toList();

            if (itemDeliveries.isEmpty()) {
                throw new IllegalStateException("배송 완료 후 반품 신청이 가능합니다. (배송 데이터 없음)");
            }

            Delivery deliveredDetail = itemDeliveries.stream()
                    .filter(d -> "DELIVERED".equals(d.getStatus()))
                    .findFirst()
                    .orElse(null);

            if (deliveredDetail == null) {
                throw new IllegalStateException("배송이 완료된 상품만 반품 신청이 가능합니다.");
            }

            if (deliveredDetail.getCompleted_at() != null && deliveredDetail.getCompleted_at().isBefore(LocalDateTime.now().minusDays(7))) {
                throw new IllegalStateException("배송 완료 후 7일이 지난 상품은 반품 신청이 불가능합니다.");
            }
        }

        // 3. 결제완료 정보 및 Toss 결제 취소 가능 여부 조회
        Payment payment = paymentRepository.findTopByOrderSeqAndStatusOrderBySeqDesc(order.getSeq(), 2)
                .orElseThrow(() -> new IllegalStateException("결제완료 정보를 찾을 수 없습니다. orderSeq=" + order.getSeq()));

        if (payment.getExternalPaymentId() == null || payment.getExternalPaymentId().isBlank()) {
            throw new IllegalStateException("토스 paymentKey가 없어 결제를 취소할 수 없습니다.");
        }

        int cancelAmount = calculateCancelAmount(targetItems);
        if (cancelAmount <= 0) {
            throw new IllegalStateException("취소할 금액이 없습니다.");
        }

        int remainPrice = getRemainPrice(order);
        if (cancelAmount > remainPrice) {
            throw new IllegalStateException(
                    "취소 금액이 남은 결제금액보다 큽니다. cancelAmount="
                            + cancelAmount
                            + ", remainPrice="
                            + remainPrice
            );
        }

        // 4. 토스 취소 API 호출 및 즉시 환불 처리
        String cancelReason = reason != null && !reason.isBlank() ? reason : "고객 반품 신청";
        try {
            requestTossCancel(payment.getExternalPaymentId(), cancelReason, cancelAmount);
        } catch (ResponseStatusException e) {
            String errorMsg = e.getReason();
            if (errorMsg != null && (errorMsg.contains("ALREADY_CANCELED_PAYMENT") || errorMsg.contains("이미 취소된 결제"))) {
                log.warn("[Toss API] Payment is already canceled. Proceeding with DB update for idempotency: {}", errorMsg);
            } else {
                throw e;
            }
        }

        // 5. 재고 복원 처리
        increaseStockForOrderItems(targetItems, order.getSeq());

        // 6. 환불 기록(Refund) 저장
        LocalDateTime now = LocalDateTime.now();
        List<Refund> refunds = targetItems.stream()
                .map(item -> createRefundEntity(item, payment, now))
                .toList();
        refundRepository.saveAll(refunds);

        // 7. 반품 사유(RefundReason) 조회 및 생성
        RefundReason refundReason = refundReasonRepository.findByReason(cancelReason)
                .orElseGet(() -> {
                    RefundReason rr = RefundReason.builder()
                            .reason(cancelReason)
                            .build();
                    return refundReasonRepository.save(rr);
                });

        // 8. 주문 상품 상태 수정 및 반품 신청서(ReturnRequest) & 반품 배송(Returns) 생성
        for (OrderItem item : targetItems) {
            item.setRefundQuantity(item.getQuantity());
            item.setRefundPrice(item.getSubTotalPrice());
            item.setItemStatus(7); // 7: 반품요청

            // ReturnRequest 생성
            String returnUid = "RET-REQ-" + snowflakeIdGenerator.nextId();
            ReturnRequest rr = ReturnRequest.builder()
                    .orderItemSeq(item.getSeq())
                    .refundReason(refundReason)
                    .returnUid(returnUid)
                    .returnName(item.getProductName())
                    .returnQuantity(item.getQuantity())
                    .status(0) // 0: 반품신청
                    .requestDate(now)
                    .zipCode(order.getZipcode())
                    .roadAddress(order.getAddress())
                    .detailAddress(order.getAddressDetail())
                    .build();
            ReturnRequest savedRr = returnRequestRepository.save(rr);

            // 해당 상품의 판매자 택배사 정보 획득
            Options opt = optionsRepository.findById(item.getOptionsSeq()).orElse(null);
            Seller sel = opt != null ? sellerRepository.findById(opt.getProduct().getSellerSeq()).orElse(null) : null;
            DeliveryCompany company = sel != null ? sel.getDeliveryCompany() : null;

            // Returns 생성 (반품 배송)
            String trackingNumber = "RET-TRK-" + snowflakeIdGenerator.nextId();
            Returns ret = Returns.builder()
                    .returnRequest(savedRr)
                    .deliveryCompany(company)
                    .status("RETURNING") // 반품 배송 중
                    .trackingNumber(trackingNumber)
                    .build();
            returnsRepository.save(ret);
        }
        orderItemRepository.saveAll(targetItems);

        // 9. 주문(Orders) 및 결제(Payment) 잔액 및 상태 업데이트
        int newTotalRefundPrice = nullToZero(order.getTotalRefundPrice()) + cancelAmount;
        int newRemainPrice = nullToZero(order.getFinalPrice()) - newTotalRefundPrice;
        if (newRemainPrice < 0) {
            newRemainPrice = 0;
        }

        order.setTotalRefundPrice(newTotalRefundPrice);
        order.setRemainPrice(newRemainPrice);

        boolean allCanceledOrReturned = allOrderItems.stream()
                .allMatch(item -> {
                    if (targetSeqs.contains(item.getSeq())) {
                        return true;
                    }
                    return item.getItemStatus() != null && (item.getItemStatus() == 6 || item.getItemStatus() == 7 || item.getItemStatus() == 8 || item.getItemStatus() == 9);
                });

        if (allCanceledOrReturned) {
            order.setOrderStatus(8); // 전체환불
            order.setPaymentStatus(5); // 전체환불
            payment.setStatus(4); // 환불완료
            payment.setFailReason(cancelReason);
            restoreCouponIfExists(order);
        } else {
            order.setOrderStatus(7); // 부분환불
            order.setPaymentStatus(4); // 부분환불
        }

        payment.setUpdateDate(now);
        ordersRepository.save(order);
        paymentRepository.save(payment);
    }
    
    /**
     * 선택 취소된 상품들이 속한 택배사 묶음 중,
     * 해당 택배사의 남은 상품이 모두 취소되는 경우에만 배송비를 환불한다.
     *
     * 예)
     * - CJ대한통운 상품 2개 중 2개 모두 취소 → CJ 배송비 환불
     * - CJ대한통운 상품 2개 중 1개만 취소 → CJ 배송비 환불 안 함
     *
     * 현재 화면 정책상 주문취소는 같은 택배사 상품을 한 번에 묶어서 취소하므로
     * 보통 선택한 택배사 배송비가 함께 환불된다.
     */
    private int calculateCancelableDeliveryFee(List<OrderItem> allOrderItems,
                                               List<OrderItem> cancelItems,
                                               Set<Long> currentCancelItemSeqSet) {

        Map<Long, DeliveryCompany> selectedCompanyMap = new LinkedHashMap<>();

        for (OrderItem item : cancelItems) {
            DeliveryCompany company = getDeliveryCompanyByOrderItem(item);

            if (company != null && company.getSeq() != null) {
                selectedCompanyMap.put(company.getSeq(), company);
            }
        }

        int deliveryFeeRefundAmount = 0;

        for (Map.Entry<Long, DeliveryCompany> entry : selectedCompanyMap.entrySet()) {
            Long companySeq = entry.getKey();
            DeliveryCompany company = entry.getValue();

            boolean allRemainingItemsOfCompanyCanceled = allOrderItems.stream()
                    .filter(item -> !isAlreadyCanceledOrReturned(item))
                    .filter(item -> {
                        DeliveryCompany itemCompany = getDeliveryCompanyByOrderItem(item);
                        return itemCompany != null
                                && itemCompany.getSeq() != null
                                && itemCompany.getSeq().equals(companySeq);
                    })
                    .allMatch(item -> currentCancelItemSeqSet.contains(item.getSeq()));

            if (allRemainingItemsOfCompanyCanceled) {
                deliveryFeeRefundAmount += nullToZero(company.getBase_delivery_fee());
            }
        }

        return deliveryFeeRefundAmount;
    }
    
    private DeliveryCompany getDeliveryCompanyByOrderItem(OrderItem item) {
        if (item == null || item.getOptionsSeq() == null) {
            return null;
        }

        Options options = optionsRepository.findById(item.getOptionsSeq())
                .orElse(null);

        if (options == null || options.getProduct() == null || options.getProduct().getSellerSeq() == null) {
            return null;
        }

        Seller seller = sellerRepository.findById(options.getProduct().getSellerSeq())
                .orElse(null);

        if (seller == null) {
            return null;
        }

        return seller.getDeliveryCompany();
    }
    
    private boolean isAlreadyCanceledOrReturned(OrderItem item) {
        if (item == null || item.getItemStatus() == null) {
            return false;
        }

        return item.getItemStatus() == 6
                || item.getItemStatus() == 7
                || item.getItemStatus() == 8
                || item.getItemStatus() == 9;
    }
}