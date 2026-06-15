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
 * memberships_log: 멤버십 변경 이력.
 *
 * type 상수:
 *  - JOIN   : 최초 가입
 *  - RENEW  : 자동 갱신 결제
 *  - CANCEL : 취소 예정 등록
 *  - EXPIRE : 만료 처리
 */
@Entity
@Table(name = "memberships_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipsLog {

    public static final String TYPE_JOIN   = "join";
    public static final String TYPE_RENEW  = "renew";
    public static final String TYPE_CANCEL = "cancel";
    public static final String TYPE_EXPIRE = "expire";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "memberships_log_seq")
    @SequenceGenerator(name = "memberships_log_seq", sequenceName = "memberships_log_seq", allocationSize = 1)
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberships_seq", nullable = false)
    private Memberships memberships;

    /** join / renew / cancel / expire */
    @Column(name = "type", length = 20, nullable = false)
    private String type;

    /** 결제 금액 (cancel/expire는 null 가능) */
    @Column(name = "amount")
    private Integer amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MembershipsLog(Member member, Memberships memberships,
                           String type, Integer amount) {
        this.member      = member;
        this.memberships = memberships;
        this.type        = type;
        this.amount      = amount;
        this.createdAt   = LocalDateTime.now();
    }
}
