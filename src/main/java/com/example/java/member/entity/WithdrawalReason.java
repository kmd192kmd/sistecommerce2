package com.example.java.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 탈퇴 사유 코드 테이블.
 */
@Entity
@Table(name = "withdrawal_reason")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalReason {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "withdrawal_reason_seq")
    @SequenceGenerator(name = "withdrawal_reason_seq", sequenceName = "withdrawal_reason_seq", allocationSize = 1)
    private Long seq;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Builder
    private WithdrawalReason(String reason) {
        this.reason = reason;
    }
}
