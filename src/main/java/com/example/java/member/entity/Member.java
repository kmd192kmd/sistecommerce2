package com.example.java.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "member_seq", allocationSize = 1)
    private Long seq;

    @Column(nullable = false, unique = true, updatable = false)
    private String username;

    private String password;
    private String name;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(unique = true)
    private String email;

    private String phone;

    @Column(unique = true)
    private String ci;

    private String di;
    private String zipcode;
    private String address;

    @Column(name = "address_detail")
    private String addressDetail;

    @Column(length = 1)
    private String gender;

    private LocalDate birth;

    @Column(nullable = false)
    private Integer status;

    @Column(nullable = false)
    private String role;

    @Column(name = "email_verified", length = 1)
    private String emailVerified;

    @Column(name = "phone_verified", length = 1)
    private String phoneVerified;

    @Column(name = "two_factor", length = 1)
    private String twoFactor;

    private String totp;

    @Column(name = "pw_changed_at")
    private LocalDateTime pwChangedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "withdrawal_requested_at")
    private LocalDateTime withdrawalRequestedAt;

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "login_type", nullable = false)
    private String loginType;

    /**
     * 소셜 로그인 신규 회원 생성용 팩토리 메서드
     * username: "{provider}_{providerId}" 형태 (예: google_109374837293)
     */
    public static Member ofSocial(String username, String nickname, String name,
                                   String email, String loginType) {
        return Member.builder()
                .username(username)
                .nickname(nickname)
                .name(name)
                .email(email)
                .loginType(loginType)
                .status(1)
                .role("ROLE_USER")
                .emailVerified("Y")
                .joinedAt(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * 소셜 재로그인 시 이름/이메일 최신 정보로 갱신
     */
    public void updateSocialInfo(String name, String email) {
        this.name      = name;
        this.email     = email;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void updateProfile(String nickname, String email, String phone) {
        this.nickname  = nickname;
        this.email     = email;
        this.phone     = phone;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.pwChangedAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public void changeStatus(Integer status) {
        this.status = status;

    }

    /** 탈퇴 신청: status 1→4(탈퇴보류중), 신청일시 기록 */
    public void markWithdrawalRequested() {
        this.status = com.example.java.member.constant.MemberStatus.WITHDRAWAL_PENDING;
        this.withdrawalRequestedAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    /** 탈퇴 복구: status 4→1(활성), 신청일시 해제 */
    public void restoreActive() {
        this.status = com.example.java.member.constant.MemberStatus.ACTIVE;
        this.withdrawalRequestedAt = null;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    /**
     * 탈퇴 확정: status 4→5(탈퇴) + 민감 개인정보 비가역 마스킹.
     * seq/row 는 유지(구매이력 연결 보존). 원본은 member_withdrawal 에 분리보관됨.
     * UNIQUE NOT NULL 컬럼(username/nickname)은 seq 기반 익명값으로, 나머지 PII 는 null 처리.
     */
    public void maskForWithdrawal() {
        this.username      = com.example.java.member.util.MaskingUtil.anonymized("WD", this.seq);
        this.nickname      = com.example.java.member.util.MaskingUtil.anonymized("탈퇴회원", this.seq);
        this.password      = null;
        this.name          = null;
        this.email         = null;
        this.phone         = null;
        this.ci            = null;
        this.di            = null;
        this.zipcode       = null;
        this.address       = null;
        this.addressDetail = null;
        this.gender        = null;
        this.birth         = null;
        this.totp          = null;
        this.status        = com.example.java.member.constant.MemberStatus.WITHDRAWN;
        this.updatedAt     = java.time.LocalDateTime.now();
    }

    /**
     * 휴면 전환: status 1→2 + 민감 개인정보 제거(분리보관 후 null).
     * 식별자(username/nickname)와 password 는 유지(로그인·복원에 필요).
     */
    public void enterDormant() {
        this.name          = null;
        this.email         = null;
        this.phone         = null;
        this.ci            = null;
        this.di            = null;
        this.zipcode       = null;
        this.address       = null;
        this.addressDetail = null;
        this.gender        = null;
        this.birth         = null;
        this.status        = com.example.java.member.constant.MemberStatus.DORMANT;
        this.updatedAt     = java.time.LocalDateTime.now();
    }

    /** 휴면 복원: status 2→1 + 분리보관 스냅샷에서 민감정보 복구 */
    public void restoreFromDormant(MemberDormant dormant) {
        this.name          = dormant.getName();
        this.email         = dormant.getEmail();
        this.phone         = dormant.getPhone();
        this.ci            = dormant.getCi();
        this.di            = dormant.getDi();
        this.zipcode       = dormant.getZipcode();
        this.address       = dormant.getAddress();
        this.addressDetail = dormant.getAddressDetail();
        this.gender        = dormant.getGender();
        this.birth         = dormant.getBirth();
        this.status        = com.example.java.member.constant.MemberStatus.ACTIVE;
        this.updatedAt     = java.time.LocalDateTime.now();
    }
}
