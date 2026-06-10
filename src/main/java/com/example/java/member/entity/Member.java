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
}
