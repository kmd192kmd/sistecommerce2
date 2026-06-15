package com.example.java.member.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;

import com.example.java.member.service.RememberMeTokenService;
import com.example.java.member.service.RememberMeTokenService.AutoLoginResult;
import com.example.java.member.util.PlatformDetector;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * remember_me_token 테이블 기반 커스텀 자동로그인 서비스.
 *
 * - 쿠키에는 원본 토큰 1개만 담는다 (DB에는 해시 저장).
 * - 로그인 성공 + 자동로그인 체크 시 토큰 발급 → 쿠키 설정 (onLoginSuccess).
 * - 자동로그인 쿠키 수신 시 검증·회전 후 인증 (processAutoLoginCookie).
 * - 실제 토큰 검증/상태확인/회전은 {@link RememberMeTokenService#consume}가 단일 트랜잭션으로 처리.
 */
@Slf4j
public class CustomRememberMeServices extends AbstractRememberMeServices {

    /** 쿠키/토큰 유효기간(초) — 토큰 서비스의 슬라이딩 기간(14일)과 맞춤 */
    private static final int VALIDITY_SECONDS = 14 * 24 * 60 * 60;

    private final RememberMeTokenService tokenService;

    public CustomRememberMeServices(String key,
                                    UserDetailsService userDetailsService,
                                    RememberMeTokenService tokenService) {
        super(key, userDetailsService);
        this.tokenService = tokenService;
    }

    /**
     * 자동로그인 쿠키 처리.
     * 검증 실패/만료/탈취/비활성 계정이면 InvalidCookieException → 쿠키 폐기 후 인증 미수행.
     */
    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        if (cookieTokens == null || cookieTokens.length != 1) {
            throw new InvalidCookieException("잘못된 자동로그인 쿠키 형식");
        }

        String rawToken    = cookieTokens[0];
        String deviceType  = PlatformDetector.detect(request);
        String fingerprint = fingerprint(request);

        AutoLoginResult result = tokenService.consume(rawToken, deviceType, fingerprint);
        if (!result.success()) {
            throw new InvalidCookieException("자동로그인 실패: " + result.reason());
        }

        // 회전된 새 토큰으로 쿠키 갱신
        setCookie(new String[] { result.newRawToken() }, VALIDITY_SECONDS, request, response);

        log.debug("자동로그인 성공 - username: {}", result.username());
        return getUserDetailsService().loadUserByUsername(result.username());
    }

    /**
     * 폼 로그인 성공 + 자동로그인 요청 시 호출.
     * 토큰을 발급하고 쿠키로 내려보낸다.
     */
    @Override
    public void onLoginSuccess(HttpServletRequest request,
                               HttpServletResponse response,
                               Authentication successfulAuthentication) {
        if (!(successfulAuthentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            log.debug("자동로그인 토큰 발급 스킵 - principal 타입: {}",
                    successfulAuthentication.getPrincipal().getClass().getSimpleName());
            return;
        }

        String deviceType  = PlatformDetector.detect(request);
        String fingerprint = fingerprint(request);

        String rawToken = tokenService.issue(userDetails.getMemberSeq(), deviceType, fingerprint);
        setCookie(new String[] { rawToken }, VALIDITY_SECONDS, request, response);

        log.debug("자동로그인 토큰 발급·쿠키 설정 - memberSeq: {}", userDetails.getMemberSeq());
    }

    /**
     * 로그아웃 시 호출 (AbstractRememberMeServices 가 LogoutHandler 로 자동 등록됨).
     * DB의 자동로그인 토큰을 전체 무효화한 뒤, 쿠키도 폐기한다.
     */
    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {
        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            tokenService.invalidateAll(userDetails.getMemberSeq());
            log.debug("로그아웃 - 자동로그인 토큰 전체 무효화. memberSeq: {}", userDetails.getMemberSeq());
        }
        super.logout(request, response, authentication); // 쿠키 폐기
    }

    /** User-Agent 기반 단순 디바이스 지문 (200자 제한) */
    private String fingerprint(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) {
            return null;
        }
        return ua.length() > 200 ? ua.substring(0, 200) : ua;
    }
}
