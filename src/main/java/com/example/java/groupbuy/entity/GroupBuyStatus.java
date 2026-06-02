package com.example.java.groupbuy.entity;

/**
 * 공동구매(group_buy) 진행 상태.
 * DB에는 @Enumerated(STRING)으로 enum 이름 그대로 저장된다. (DDL: status varchar2(20))
 */
public enum GroupBuyStatus {
    SCHEDULED,  // 시작 전 (등록되어 시작 대기)
    ONGOING,    // 진행 중 (참여 모집)
    CONFIRMED,  // 확정 마감 (최소 인원 달성)
    FAILED,     // 무산 (최소 인원 미달, 전원 환불)
    STOPPED     // 관리자 강제 중단
}
