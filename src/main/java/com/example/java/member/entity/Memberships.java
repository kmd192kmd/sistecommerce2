package com.example.java.member.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "memberships")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Memberships {

    /** status 상수 */
    public static final String STATUS_ACTIVE   = "active";
    public static final String STATUS_CANCELED = "canceled";  // 취소 예정 (만료일까지 유지)
    public static final String STATUS_EXPIRED  = "expired";
    public static final String STATUS_NONE     = "none";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "memberships_seq")
    @SequenceGenerator(name = "memberships_seq", sequenceName = "memberships_seq", allocationSize = 1)
    private Long seq;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @Column(name = "billing_key", length = 200)
    private String billingKey;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "next_billing_at")
    private LocalDateTime nextBillingAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    // -------------------------------------------------------------------------
    // 상태 변경 메서드
    // -------------------------------------------------------------------------

    /** 가입 / 갱신 활성화 */
    public void activate(String billingKey, LocalDateTime expireAt, LocalDateTime nextBillingAt) {
        this.billingKey     = billingKey;
        this.status         = STATUS_ACTIVE;
        this.startedAt      = LocalDateTime.now();
        this.expireAt       = expireAt;
        this.nextBillingAt  = nextBillingAt;
        this.canceledAt     = null;
    }

    /** 취소 예정 (만료일까지 유지, 자동갱신 중단) */
    public void scheduleCancel() {
        this.status         = STATUS_CANCELED;
        this.nextBillingAt  = null;
        this.canceledAt     = LocalDateTime.now();
    }

    /** 만료 처리 */
    public void expire() {
        this.status        = STATUS_EXPIRED;
        this.nextBillingAt = null;
    }

    /** 빌링키 갱신 */
    public void updateBillingKey(String billingKey) {
        this.billingKey = billingKey;
    }
}
