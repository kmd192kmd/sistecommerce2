package com.example.java.member.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FindUsernameResultDto {

    private String maskedUsername;  // 마스킹 처리된 아이디
    private LocalDateTime joinedAt; // 가입일
}
