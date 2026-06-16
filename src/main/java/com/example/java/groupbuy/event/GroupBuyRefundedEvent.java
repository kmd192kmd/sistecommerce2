package com.example.java.groupbuy.event;

/**
 * 참여 취소/무산에 따른 PG 환불이 완료된 사실. 환불완료 알림(REFUND_DONE)의 트리거.
 */
public record GroupBuyRefundedEvent(Long memberSeq, Long participationSeq) {
}
