package com.example.java.member.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
	
}
