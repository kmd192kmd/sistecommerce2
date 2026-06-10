package com.example.java.member.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.java.member.entity.LoginLog;
import com.example.java.member.repository.MemberRepository;
import com.example.java.member.service.LoginLogService;
import com.example.java.member.service.SessionManagementService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FormLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SessionManagementService sessionManagementService;
    private final LoginLogService loginLogService;
    private final MemberRepository memberRepository;

    public FormLoginSuccessHandler(SessionManagementService sessionManagementService,
                                   LoginLogService loginLogService,
                                   MemberRepository memberRepository) {
        this.sessionManagementService = sessionManagementService;
        this.loginLogService = loginLogService;
        this.memberRepository = memberRepository;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        sessionManagementService.register(request, request.getSession(), userDetails.getUsername());

        memberRepository.findById(userDetails.getMemberSeq()).ifPresent(member ->
                loginLogService.logSuccess(member, request, LoginLog.TYPE_FORM));

        log.debug("일반 로그인 성공: {}", userDetails.getUsername());
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
