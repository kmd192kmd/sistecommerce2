package com.example.java.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginUserDto {

    private Long seq;
    private String username;
    private String password;
    private String role;
    private Integer status;
    private String loginType;
    private Integer admRole;
}