package com.example.java.member.entity;

import java.time.LocalDate;
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
 * 탈퇴 회원 분리보관 테이블.
 *
 * 탈퇴 신청 시 member 의 (마스킹 전) 원본 식별정보를 복사해 별도 보관한다.
 * - 전자상거래법상 거래주체 식별정보 보존(분리 보관) 용도.
 * - withdrawal_yn='N': 탈퇴보류중(유예), 'Y': 탈퇴확정(member 마스킹 완료).
 * - scheduled_delete_at: 보류중이면 유예 만료일(신청+3일), 확정 후이면 최종 파기 예정일(확정+5년).
 */
@Entity
@Table(name = "member_withdrawal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWithdrawal {

    /** withdrawal_yn 상수 */
    public static final String YN_Y = "Y";
    public static final String YN_N = "N";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_withdrawal_seq")
    @SequenceGenerator(name = "member_withdrawal_seq", sequenceName = "member_withdrawal_seq", allocationSize = 1)
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "withdrawal_reason_seq", nullable = false)
    private WithdrawalReason withdrawalReason;

    // --- 신청 시점의 원본 식별정보 스냅샷 (분리 보관) ---
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password", length = 100)
    private String password;

    @Column(name = "name", length = 60)
    private String name;

    @Column(name = "nickname", length = 60)
    private String nickname;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "ci", length = 100)
    private String ci;

    @Column(name = "di", length = 100)
    private String di;

    @Column(name = "zipcode", length = 5)
    private String zipcode;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "address_detail", length = 100)
    private String addressDetail;

    @Column(name = "gender", length = 1)
    private String gender;

    @Column(name = "birth")
    private LocalDate birth;

    // --- 탈퇴 진행 상태 ---
    @Column(name = "withdrawal_requested_at", nullable = false, updatable = false)
    private LocalDateTime withdrawalRequestedAt;

    @Column(name = "withdrawal_completed_at")
    private LocalDateTime withdrawalCompletedAt;

    @Column(name = "withdrawal_yn", nullable = false, length = 1)
    private String withdrawalYn;

    @Column(name = "scheduled_delete_at", nullable = false)
    private LocalDate scheduledDeleteAt;

    @Builder
    private MemberWithdrawal(Member member, WithdrawalReason withdrawalReason,
                            String username, String password, String name, String nickname,
                            String email, String phone, String ci, String di,
                            String zipcode, String address, String addressDetail,
                            String gender, LocalDate birth,
                            LocalDateTime withdrawalRequestedAt, LocalDateTime withdrawalCompletedAt,
                            String withdrawalYn, LocalDate scheduledDeleteAt) {
        this.member               = member;
        this.withdrawalReason     = withdrawalReason;
        this.username             = username;
        this.password             = password;
        this.name                 = name;
        this.nickname             = nickname;
        this.email                = email;
        this.phone                = phone;
        this.ci                   = ci;
        this.di                   = di;
        this.zipcode              = zipcode;
        this.address              = address;
        this.addressDetail        = addressDetail;
        this.gender               = gender;
        this.birth                = birth;
        this.withdrawalRequestedAt = withdrawalRequestedAt != null ? withdrawalRequestedAt : LocalDateTime.now();
        this.withdrawalCompletedAt = withdrawalCompletedAt;
        this.withdrawalYn         = withdrawalYn != null ? withdrawalYn : YN_N;
        this.scheduledDeleteAt    = scheduledDeleteAt;
    }

    /**
     * 탈퇴 신청 시 member 원본 스냅샷으로 분리보관 레코드 생성.
     *
     * @param scheduledDeleteAt 유예 만료일(신청+3일)
     */
    public static MemberWithdrawal request(Member member, WithdrawalReason reason, LocalDate scheduledDeleteAt) {
        return MemberWithdrawal.builder()
                .member(member)
                .withdrawalReason(reason)
                .username(member.getUsername())
                .password(member.getPassword())
                .name(member.getName())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .phone(member.getPhone())
                .ci(member.getCi())
                .di(member.getDi())
                .zipcode(member.getZipcode())
                .address(member.getAddress())
                .addressDetail(member.getAddressDetail())
                .gender(member.getGender())
                .birth(member.getBirth())
                .withdrawalRequestedAt(LocalDateTime.now())
                .withdrawalYn(YN_N)
                .scheduledDeleteAt(scheduledDeleteAt)
                .build();
    }

    public boolean isPending() {
        return YN_N.equals(withdrawalYn);
    }

    /**
     * 탈퇴 확정 처리(member 마스킹 완료 시점).
     *
     * @param finalDeleteAt 최종 파기 예정일(확정+5년)
     */
    public void complete(LocalDate finalDeleteAt) {
        this.withdrawalYn          = YN_Y;
        this.withdrawalCompletedAt = LocalDateTime.now();
        this.scheduledDeleteAt     = finalDeleteAt;
    }

    /**
     * 5년 경과 최종 파기: 보관 중인 원본 식별정보를 비가역 마스킹값으로 덮어쓴다.
     * member row 는 건드리지 않는다(이미 확정 시 마스킹됨).
     */
    public void purge(String maskedValue) {
        this.username      = maskedValue;
        this.password      = null;
        this.name          = maskedValue;
        this.nickname      = maskedValue;
        this.email         = maskedValue;
        this.phone         = maskedValue;
        this.ci            = maskedValue;
        this.di            = maskedValue;
        this.zipcode       = null;
        this.address       = maskedValue;
        this.addressDetail = null;
        this.gender        = null;
        this.birth         = null;
    }
}
