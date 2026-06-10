package com.example.java.groupbuy.entity;

/**
 * 공구 참여(participation) 상태.
 * DB에는 @Enumerated(STRING)으로 enum 이름 그대로 저장된다. (DDL: status varchar2(20))
 *
 * 상태 흐름:
 *  - 정규 참여(즉시 결제): 바로 PARTICIPATING
 *  - 대기열 승격자: PAYMENT_PENDING(결제대기) → 결제 완료 시 PARTICIPATING / 기한 초과 시 EXPIRED
 *  - 마감 시: PARTICIPATING → CONFIRMED(성사) / FAILED(무산)
 *  - 사용자 취소: CANCELLED
 */
public enum ParticipationStatus {
    PAYMENT_PENDING,  // 결제대기 (대기열에서 승격되어 결제기한 내 결제 대기 중)
    PARTICIPATING,    // 참여중 (결제 완료, 마감 전)
    CONFIRMED,        // 확정 (마감 시 최소 인원 달성 → 성사)
    CANCELLED,        // 취소 (사용자가 마감 전 참여 취소)
    FAILED,           // 무산 (마감 시 최소 인원 미달 → 전원 환불)
    EXPIRED           // 만료 (승격 후 결제기한 내 미결제 → 자격 소멸. 점유는 복구되고 다음 대기자 승격)
}
