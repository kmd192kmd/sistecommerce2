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

@Entity
@Table(name = "login_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginLog {

    /** result 상수 */
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_FAIL    = 1;

    /** fail_reason 상수 */
    public static final String FAIL_PW_MISMATCH   = "PW_MISSMATCH";
    public static final String FAIL_ACCOUNT_LOCK  = "ACCOUNT_LOCK";

    /** login_type 상수 */
    public static final int TYPE_FORM = 0; // 일반 로그인
    public static final int TYPE_SSO  = 1; // 소셜(SSO) 로그인

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "login_log_seq")
    @SequenceGenerator(name = "login_log_seq", sequenceName = "login_log_seq", allocationSize = 1)
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @Column(name = "ip_address", nullable = false, length = 20)
    private String ipAddress;

    @Column(name = "device_type", nullable = false, length = 45)
    private String deviceType;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    /** 0: 성공, 1: 실패 */
    @Column(name = "result", nullable = false)
    private int result;

    /** PW_MISSMATCH / ACCOUNT_LOCK */
    @Column(name = "fail_reason", length = 50)
    private String failReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 0: 일반, 1: SSO */
    @Column(name = "login_type", nullable = false)
    private int loginType;

    @Builder
    private LoginLog(Member member, String ipAddress, String deviceType,
                     String userAgent, int result, String failReason,
                     LocalDateTime createdAt, int loginType) {
        this.member     = member;
        this.ipAddress  = ipAddress;
        this.deviceType = deviceType;
        this.userAgent  = userAgent;
        this.result     = result;
        this.failReason = failReason;
        this.createdAt  = createdAt != null ? createdAt : LocalDateTime.now();
        this.loginType  = loginType;
    }
}
