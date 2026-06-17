package com.example.java.groupbuy.payment;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.Orders;
import com.example.java.orders.repository.OrderItemRepository;
import com.example.java.orders.repository.OrdersRepository;
import com.example.java.orders.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 공구 환불 실제 구현체 (취소/무산/강제중단 → 토스 결제취소).
 *
 * participation_seq로 원주문(order_item)을 찾아, 그 중 '결제완료' 주문을 결제 도메인의
 * {@link PaymentService#cancelPayment(Long, Long)}(토스 취소 + 환불기록 + 주문상태 변경)에 위임한다.
 * 환불액은 order_item.final_price 스냅샷 그대로 처리되므로 공구 쪽에서 할인율을 역산하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuyRefundAdapter implements GroupBuyPaymentPort {

    private final OrderItemRepository orderItemRepository;
    private final OrdersRepository ordersRepository;
    private final PaymentService paymentService;

    /** 결제완료를 나타내는 orders.payment_status 값 (orders 도메인 규약). */
    private static final int PAYMENT_STATUS_PAID = 2;

    @Override
    public void refund(Long participationSeq) {
        // 한 참여로 만들어진 주문상품들 중 '결제완료된' 주문 하나를 찾아 취소한다.
        // - 미결제 이탈로 생긴 결제대기 주문은 환불 대상이 아니다.
        // - 이미 취소된 주문이면 payment_status가 2가 아니므로 건너뛴다 → 중복 호출돼도 PG 취소는 1회만 (NFR-004).
        List<OrderItem> items = orderItemRepository.findByParticipationSeq(participationSeq);
        for (OrderItem item : items) {
            Orders order = ordersRepository.findById(item.getOrderSeq()).orElse(null);
            if (order == null) {
                continue;
            }
            if (order.getPaymentStatus() != null && order.getPaymentStatus() == PAYMENT_STATUS_PAID) {
                paymentService.cancelPayment(order.getSeq(), order.getMemberSeq());
                return;
            }
        }
        // 결제완료 주문이 없으면(이미 환불됐거나 결제 전) 환불할 것이 없다.
        log.info("[공구 환불] 결제완료 주문이 없어 환불 생략 participationSeq={}", participationSeq);
    }
}
