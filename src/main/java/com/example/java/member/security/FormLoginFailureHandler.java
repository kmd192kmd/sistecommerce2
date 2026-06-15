package com.example.java.member.security;

import java.io.IOException;

import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.example.java.member.entity.LoginLog;
import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;
import com.example.java.member.service.LoginLogService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 폼 로그인 실패 핸들러.
 *
 * 실패 원인에 따라 fail_reason 결정:
 *  - LockedException     → ACCOUNT_LOCK
 *  - 그 외 (비밀번호 불일치 등) → PW_MISSMATCH
 *
 * member가 존재하지 않는 계정(username 오타 등)이면 로그 저장 스킵.
 */
@Slf4j
@Component
public class FormLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginLogService loginLogService;
    private final MemberRepository memberRepository;

    public FormLoginFailureHandler(LoginLogService loginLogService, MemberRepository memberRepository) {
        this.loginLogService = loginLogService;
        this.memberRepository = memberRepository;
        setDefaultFailureUrl("/member/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        String failReason = (exception instanceof LockedException)
                ? LoginLog.FAIL_ACCOUNT_LOCK
                : LoginLog.FAIL_PW_MISMATCH;

        if (username != null && !username.isBlank()) {
            Member member = memberRepository.findByUsername(username).orElse(null);
            loginLogService.logFailure(member, request, failReason);
        }

        log.debug("로그인 실패 - username: {}, reason: {}", username, failReason);
        super.onAuthenticationFailure(request, response, exception);
    }
}
