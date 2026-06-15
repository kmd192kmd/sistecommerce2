package com.example.java.groupbuy.payment;

/**
 * 공구 환불 경계(포트).
 *
 * 결제(주문 생성)는 토스 2단계 흐름으로 바뀌어 {@code OrdersCommandService.createGroupBuyOrder} +
 * 토스 결제창 + {@code PaymentService.confirmPayment}가 담당하므로, 이 포트에는 더 이상 결제(pay)가 없다.
 * 남은 건 취소/무산/강제중단 시의 환불뿐이라, 이 포트는 '환불 경계'로만 쓴다.
 * 구현체는 {@link GroupBuyRefundAdapter}(participation_seq로 원주문을 찾아 토스 취소에 위임).
 */
public interface GroupBuyPaymentPort {

    /**
     * 공구 참여 환불 (취소/무산/강제중단). participationSeq 로 원주문(order_item.participation_seq)을 찾아
     * PG 결제취소 + 환불 처리한다 (payment.status=환불완료, orders.order_status=주문취소).
     *
     * <p>★ 환불액 = 구매 시점 order_item.final_price 스냅샷 그대로. 공구는 1인1상품·수량1·전액환불
     * (부분환불/쿠폰/핫딜 없음)이므로 '할인율 역산' 로직이 필요 없다. group_buy를 재조회해 다시 계산하지 말 것.
     *
     * <p>멱등: 호출측이 participation 상태로 멱등을 보장하지만, 구현체도 이미 환불된 건이면
     * 실제 PG 취소는 1회만 수행해야 한다 (NFR-004).
     *
     * @param participationSeq 환불 대상 공구 참여 seq
     */
    void refund(Long participationSeq);
}
