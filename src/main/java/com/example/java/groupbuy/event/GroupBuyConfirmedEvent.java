package com.example.java.groupbuy.event;

/**
 * 마감 시 공구가 확정되어 참여가 CONFIRMED된 사실. 확정 알림(GROUP_BUY_CONFIRMED)의 트리거.
 */
public record GroupBuyConfirmedEvent(Long memberSeq, Long participationSeq) {
}
