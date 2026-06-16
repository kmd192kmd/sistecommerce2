package com.example.java.groupbuy.event;

/**
 * 결제 완료로 참여가 PARTICIPATING으로 확정된 사실. 결제완료 알림(PAYMENT_DONE)의 트리거.
 */
public record GroupBuyPaymentDoneEvent(Long memberSeq, Long participationSeq) {
}
