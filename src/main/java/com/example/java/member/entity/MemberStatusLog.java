package com.example.java.member.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 상태 전환 이력.
 *
 * 휴면 전환/복원, 탈퇴 신청/복구/확정 등 status 변경 시마다 1건 기록한다.
 * - adminSeq: 시스템(스케줄러) 처리면 null, 관리자 처리면 admin seq.
 */
@Entity
@Table(name = "member_status_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_status_log_seq")
    @SequenceGenerator(name = "member_status_log_seq", sequenceName = "member_status_log_seq", allocationSize = 1)
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    /** 시스템 처리면 null, 관리자 처리면 admin seq (FK는 DB에서 보장) */
    @Column(name = "admin_seq")
    private Long adminSeq;

    @Column(name = "prev_status", nullable = false)
    private int prevStatus;

    @Column(name = "new_status", nullable = false)
    private int newStatus;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Builder
    private MemberStatusLog(Member member, Long adminSeq, int prevStatus,
                           int newStatus, String reason, LocalDateTime changedAt) {
        this.member     = member;
        this.adminSeq   = adminSeq;
        this.prevStatus = prevStatus;
        this.newStatus  = newStatus;
        this.reason     = reason;
        this.changedAt  = changedAt != null ? changedAt : LocalDateTime.now();
    }

    /** 시스템(스케줄러) 처리에 의한 상태 전환 기록 */
    public static MemberStatusLog system(Member member, int prevStatus, int newStatus, String reason) {
        return MemberStatusLog.builder()
                .member(member)
                .adminSeq(null)
                .prevStatus(prevStatus)
                .newStatus(newStatus)
                .reason(reason)
                .build();
    }

    /** 관리자 처리에 의한 상태 전환 기록 */
    public static MemberStatusLog byAdmin(Member member, Long adminSeq, int prevStatus, int newStatus, String reason) {
        return MemberStatusLog.builder()
                .member(member)
                .adminSeq(adminSeq)
                .prevStatus(prevStatus)
                .newStatus(newStatus)
                .reason(reason)
                .build();
    }
}
