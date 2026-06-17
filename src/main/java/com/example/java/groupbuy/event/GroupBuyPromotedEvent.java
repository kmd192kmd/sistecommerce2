package com.example.java.groupbuy.event;

/**
 * 대기열에서 결제대기로 승격된 사실. 승격 알림(GROUP_BUY_PROMOTED)의 트리거.
 * 커밋 경계를 넘으므로 엔티티가 아닌 값(memberSeq, participationSeq)만 담는다.
 */
public record GroupBuyPromotedEvent(Long memberSeq, Long participationSeq) {
}
