package com.example.java.orders.event;

import java.util.List;

/**
 * 주문 결제 완료 이벤트. 결제 도메인(PaymentService.confirmPayment)이 결제 확정 후 발행한다.
 * <p>
 * Kafka Consumer(PaymentKafkaConsumer)가 이 이벤트를 받아:
 * - 공구 주문: 공구 참여 확정 (confirmAfterPayment)
 * - 일반 주문: 배송 생성 (DeliveryService.createDelivery)
 * - 쿠폰: 사용 처리 (MemberCoupon.use)
 * - 장바구니: 주문 완료된 상품 삭제
 * 위 작업들을 수행한다.
 *
 * @param eventId           멱등성 보장을 위한 고유 이벤트 ID
 * @param orderSeq          주문 seq
 * @param memberSeq         주문자 회원 seq
 * @param memberCouponSeq   사용된 쿠폰 seq (없으면 null)
 * @param isGroupBuyOrder   공구 주문 여부 (true이면 배송 생성 스킵)
 * @param orderSource       주문 소스 ("DIRECT" 또는 "CART")
 * @param recipientName     수령인 이름
 * @param recipientPhone    수령인 연락처
 * @param requestMemo       배송 요청사항
 * @param participationSeqs 결제된 공구 참여 seq 목록 (공구 주문이 아니면 빈 리스트)
 */
public record OrderPaidEvent(
        String eventId,
        Long orderSeq,
        Long memberSeq,
        Long memberCouponSeq,
        boolean isGroupBuyOrder,
        String orderSource,
        String recipientName,
        String recipientPhone,
        String requestMemo,
        List<Long> participationSeqs
) {
    /**
     * 간편 생성자: 공구 주문 이벤트 (기존 공구 흐름과의 호환성 유지용).
     */
    public OrderPaidEvent(List<Long> participationSeqs) {
        this(
                java.util.UUID.randomUUID().toString(),
                null, null, null,
                true,
                null, null, null, null,
                participationSeqs
        );
    }
}
