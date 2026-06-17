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
 * 자동로그인(remember-me) 영속 토큰.
 *
 * - 원본 토큰은 쿠키로만 내려보내고, DB에는 해시(token_hash)만 저장한다.
 * - used_yn: 1회용 토큰 정책. 자동로그인에 사용되면 'Y'로 마킹하고 새 토큰으로 회전한다.
 * - 이미 사용된(또는 만료된) 토큰이 재제출되면 탈취 의심 → 해당 회원 토큰 전체 무효화에 활용.
 */
@Entity
@Table(name = "remember_me_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RememberMeToken {

    /** used_yn 상수 */
    public static final String USED_Y = "Y";
    public static final String USED_N = "N";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "remember_me_token_seq")
    @SequenceGenerator(name = "remember_me_token_seq", sequenceName = "remember_me_token_seq", allocationSize = 1)
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    /** 원본 토큰의 해시(SHA-256 등). 원본은 저장하지 않는다. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 200)
    private String tokenHash;

    @Column(name = "device_type", nullable = false, length = 100)
    private String deviceType;

    @Column(name = "device_fingerprint", length = 200)
    private String deviceFingerprint;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    /** 'Y' / 'N' — 1회용 토큰 사용 여부 */
    @Column(name = "used_yn", nullable = false, length = 1)
    private String usedYn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private RememberMeToken(Member member, String tokenHash, String deviceType,
                           String deviceFingerprint, LocalDateTime expireAt,
                           String usedYn, LocalDateTime createdAt) {
        this.member            = member;
        this.tokenHash         = tokenHash;
        this.deviceType        = deviceType;
        this.deviceFingerprint = deviceFingerprint;
        this.expireAt          = expireAt;
        this.usedYn            = usedYn != null ? usedYn : USED_N;
        this.createdAt         = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /** 만료 시각 경과 여부 */
    public boolean isExpired() {
        return expireAt == null || expireAt.isBefore(LocalDateTime.now());
    }

    /** 이미 사용된 토큰인지 */
    public boolean isUsed() {
        return USED_Y.equals(usedYn);
    }

    /** 자동로그인에 사용됨 → 1회용 마킹 */
    public void markUsed() {
        this.usedYn = USED_Y;
    }
}
