package com.example.java.member.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.java.member.entity.Member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {

	private Long seq;
    private String username;
    private String password;
    private String name;
    private String nickname;
    private String email;
    private String phone;
    private String zipcode;
    private String address;
    private String addressDetail;
    private String gender;
    private LocalDate birth;
    private Integer status;
    private String role;
    private String emailVerified;
    private String phoneVerified;
    private String loginType;
    private LocalDateTime joinedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // 회원가입 시 마케팅 수신 동의 여부 (notification_preferences 테이블에 저장)
    private Boolean marketing;

    // Entity → DTO
    public static MemberDto from(Member member) {
        return MemberDto.builder()
                .seq(member.getSeq())
                .username(member.getUsername())
                .password(member.getPassword())
                .name(member.getName())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .phone(member.getPhone())
                .zipcode(member.getZipcode())
                .address(member.getAddress())
                .addressDetail(member.getAddressDetail())
                .gender(member.getGender())
                .birth(member.getBirth())
                .status(member.getStatus())
                .role(member.getRole())
                .emailVerified(member.getEmailVerified())
                .phoneVerified(member.getPhoneVerified())
                .loginType(member.getLoginType())
                .joinedAt(member.getJoinedAt())
                .updatedAt(member.getUpdatedAt())
                .lastLoginAt(member.getLastLoginAt())
                .build();
    }

    // DTO → Entity
    public Member toEntity() {
        return Member.builder()
                .username(this.username)
                .password(this.password)
                .name(this.name)
                .nickname(this.nickname)
                .email(this.email)
                .phone(this.phone)
                .zipcode(this.zipcode)
                .address(this.address)
                .addressDetail(this.addressDetail)
                .gender(this.gender)
                .birth(this.birth)
                .status(this.status)
                .role(this.role)
                .emailVerified(this.emailVerified)
                .phoneVerified(this.phoneVerified)
                .joinedAt(this.joinedAt)
                .updatedAt(this.updatedAt)
                .loginType(this.loginType)
                .build();
    }
	
}
