package com.example.java.member.service;

import java.util.Map;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import com.example.java.member.util.PlatformDetector;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 플랫폼별 세션 관리 서비스.
 *
 * 동작 방식:
 *  1. 현재 요청의 플랫폼(PC/ANDROID/IOS) 감지
 *  2. Redis에서 동일 회원의 세션 목록 조회
 *  3. 동일 플랫폼 기존 세션에 SSE로 강제 로그아웃 알림 전송 (B-5에서 연결)
 *  4. 기존 세션 Redis에서 삭제
 *  5. 현재 세션에 플랫폼 attribute 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagementService {

    public static final String PLATFORM_ATTR = "PLATFORM";

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    /**
     * 로그인 성공 후 호출.
     * 동일 플랫폼 기존 세션을 무효화하고 현재 세션에 플랫폼을 저장.
     *
     * @param request  현재 HTTP 요청 (UserAgent 파싱용)
     * @param session  현재 로그인 세션
     * @param username 로그인한 회원 username (세션 인덱스 조회 키)
     */
    public void register(HttpServletRequest request, HttpSession session, String username) {
        String platform = PlatformDetector.detect(request);

        // Redis에서 해당 회원의 모든 세션 조회
        Map<String, ? extends Session> sessions = sessionRepository.findByPrincipalName(username);

        sessions.forEach((sessionId, s) -> {
            String storedPlatform = s.getAttribute(PLATFORM_ATTR);

            // 동일 플랫폼이고 현재 세션이 아닌 경우 무효화
            if (platform.equals(storedPlatform) && !sessionId.equals(session.getId())) {
                log.info("동일 플랫폼 세션 강제 종료 - user: {}, platform: {}, sessionId: {}",
                        username, platform, sessionId);
                // TODO: B-5에서 SSE 알림 연결 (sessionId에 강제 로그아웃 이벤트 전송)
                sessionRepository.deleteById(sessionId);
            }
        });

        // 현재 세션에 플랫폼 저장
        session.setAttribute(PLATFORM_ATTR, platform);
    }
}
