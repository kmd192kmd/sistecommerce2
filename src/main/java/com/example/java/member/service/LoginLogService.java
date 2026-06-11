package com.example.java.member.service;

import com.example.java.member.entity.LoginLog;
import com.example.java.member.entity.Member;
import com.example.java.member.repository.LoginLogRepository;
import com.example.java.member.repository.MemberRepository;
import com.example.java.member.util.PlatformDetector;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 이력 기록 서비스.
 *
 * - logSuccess : 로그인 성공 시 호출 (FormLogin / SSO 공통)
 * - logFailure : 로그인 실패 시 호출 (member가 존재하는 경우에만 저장 가능)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;
    private final MemberRepository memberRepository;

    /**
     * 로그인 성공 시 마지막 접속일시(last_login_at) 갱신. 휴면 판정 기준이 된다.
     */
    @Transactional
    public void recordLastLogin(Long memberSeq) {
        memberRepository.findById(memberSeq).ifPresent(Member::recordLogin);
    }

    /**
     * 로그인 성공 로그 저장.
     *
     * @param member    로그인한 회원
     * @param request   HTTP 요청 (IP, UserAgent 추출용)
     * @param loginType LoginLog.TYPE_FORM(0) 또는 LoginLog.TYPE_SSO(1)
     */
    public void logSuccess(Member member, HttpServletRequest request, int loginType) {
        loginLogRepository.save(buildLog(member, request, LoginLog.RESULT_SUCCESS, null, loginType));
        log.debug("로그인 성공 로그 저장 - member: {}, loginType: {}", member.getUsername(), loginType);
    }

    /**
     * 로그인 실패 로그 저장.
     * member가 null인 경우(존재하지 않는 계정) FK 제약으로 저장 불가 → 스킵.
     *
     * @param member     조회된 회원 (없으면 null)
     * @param request    HTTP 요청
     * @param failReason LoginLog.FAIL_PW_MISMATCH 또는 LoginLog.FAIL_ACCOUNT_LOCK
     */
    public void logFailure(Member member, HttpServletRequest request, String failReason) {
        if (member == null) {
            log.debug("존재하지 않는 계정 로그인 시도 - 로그 저장 스킵");
            return;
        }
        loginLogRepository.save(buildLog(member, request, LoginLog.RESULT_FAIL, failReason, LoginLog.TYPE_FORM));
        log.debug("로그인 실패 로그 저장 - member: {}, reason: {}", member.getUsername(), failReason);
    }

    // -------------------------------------------------------------------------

    private LoginLog buildLog(Member member, HttpServletRequest request,
                              int result, String failReason, int loginType) {
        String ip        = extractIp(request);
        String deviceType = PlatformDetector.detect(request);
        String userAgent  = request.getHeader("User-Agent");

        if (userAgent != null && userAgent.length() > 300) {
            userAgent = userAgent.substring(0, 300);
        }

        return LoginLog.builder()
                .member(member)
                .ipAddress(ip)
                .deviceType(deviceType)
                .userAgent(userAgent)
                .result(result)
                .failReason(failReason)
                .loginType(loginType)
                .build();
    }

    /**
     * X-Forwarded-For 헤더 우선, 없으면 RemoteAddr 사용.
     * IP가 20자를 초과하면 잘라냄 (DDL varchar2(20) 제약).
     */
    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim(); // 프록시 체인의 첫 번째 IP
        }
        return ip.length() > 20 ? ip.substring(0, 20) : ip;
    }
}
