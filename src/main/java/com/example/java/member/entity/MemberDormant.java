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
 * 휴면 회원 분리보관 테이블.
 *
 * 휴면 전환 시 member 의 민감 식별정보를 복사해 별도 보관한다(평문 분리보관).
 * - member 에는 식별자(username/nickname)·비민감 정보만 남기고 민감정보는 null 처리.
 * - 복원 시 이 레코드의 값을 member 로 되돌리고 레코드는 삭제한다.
 * - auto_dormant_yn: 'Y' 자동(스케줄러), 'N' 수동(관리자).
 */
@Entity
@Table(name = "member_dormant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberDormant {

    /** auto_dormant_yn 상수 */
    public static final String AUTO_Y = "Y";
    public static final String AUTO_N = "N";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_dormant_seq")
    @SequenceGenerator(name = "member_dormant_seq", sequenceName = "member_dormant_seq", allocationSize = 1)
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    // --- 휴면 전환 시점의 원본 식별정보 스냅샷 (분리 보관) ---
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

    // --- 휴면 메타 ---
    @Column(name = "dormant_at", nullable = false, updatable = false)
    private LocalDateTime dormantAt;

    @Column(name = "dormant_reason", nullable = false, length = 100)
    private String dormantReason;

    @Column(name = "auto_dormant_yn", nullable = false, length = 1)
    private String autoDormantYn;

    @Column(name = "scheduled_delete_at", nullable = false)
    private LocalDate scheduledDeleteAt;

    @Builder
    private MemberDormant(Member member, String username, String password, String name,
                         String nickname, String email, String phone, String ci, String di,
                         String zipcode, String address, String addressDetail, String gender,
                         LocalDate birth, LocalDateTime dormantAt, String dormantReason,
                         String autoDormantYn, LocalDate scheduledDeleteAt) {
        this.member            = member;
        this.username          = username;
        this.password          = password;
        this.name              = name;
        this.nickname          = nickname;
        this.email             = email;
        this.phone             = phone;
        this.ci                = ci;
        this.di                = di;
        this.zipcode           = zipcode;
        this.address           = address;
        this.addressDetail     = addressDetail;
        this.gender            = gender;
        this.birth             = birth;
        this.dormantAt         = dormantAt != null ? dormantAt : LocalDateTime.now();
        this.dormantReason     = dormantReason;
        this.autoDormantYn     = autoDormantYn != null ? autoDormantYn : AUTO_Y;
        this.scheduledDeleteAt = scheduledDeleteAt;
    }

    /**
     * 휴면 전환 시 member 원본 스냅샷으로 분리보관 레코드 생성.
     *
     * @param reason            휴면 사유
     * @param scheduledDeleteAt 휴면 상태 보관 만료(삭제) 예정일
     */
    public static MemberDormant of(Member member, String reason, boolean auto, LocalDate scheduledDeleteAt) {
        return MemberDormant.builder()
                .member(member)
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
                .dormantAt(LocalDateTime.now())
                .dormantReason(reason)
                .autoDormantYn(auto ? AUTO_Y : AUTO_N)
                .scheduledDeleteAt(scheduledDeleteAt)
                .build();
    }
}
