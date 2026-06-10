package com.example.java.admin.dto;

import java.time.LocalDateTime;

import lombok.Data;


@Data
public class AdminMemberDto {
    private Long seq;
    private String name;
    private String username;
    private String nickname;
    private String email;
    private LocalDateTime joinedAt;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private Long purchaseCount;
    private String membershipStatus;

    public AdminMemberDto(Long seq, String name, String username, String nickname, String email,
                          LocalDateTime joinedAt, Integer status, LocalDateTime lastLoginAt,
                          Long purchaseCount, String membershipStatus) {
        this.seq = seq;
        this.name = name;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.joinedAt = joinedAt;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
        this.purchaseCount = purchaseCount != null ? purchaseCount : 0L;
        this.membershipStatus = membershipStatus;
    }
}
