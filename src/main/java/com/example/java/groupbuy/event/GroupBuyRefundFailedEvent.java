package com.example.java.groupbuy.event;

/**
 * 무산 환불 처리 중 PG 환불이 실패한 사실. 환불실패 알림(REFUND_FAILED)의 트리거.
 */
public record GroupBuyRefundFailedEvent(Long memberSeq, Long participationSeq) {
}
