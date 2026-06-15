package com.example.java.groupbuy.event;

/**
 * 마감 시 최소 인원 미달로 공구가 무산되어 참여가 FAILED된 사실. 무산 알림(GROUP_BUY_FAILED)의 트리거.
 */
public record GroupBuyFailedEvent(Long memberSeq, Long participationSeq) {
}
