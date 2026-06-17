package com.example.java.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberSearchDto {
    private Long seq;
    private String name;     // 또는 nickname
    private String username; // 아이디
}