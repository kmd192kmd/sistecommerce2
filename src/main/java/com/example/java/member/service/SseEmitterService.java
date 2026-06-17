package com.example.java.member.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

/**
 * SSE(Server-Sent Events) 연결 관리 서비스.
 *
 * sessionId → SseEmitter 맵으로 관리.
 * 강제 로그아웃 시 해당 sessionId의 emitter에 "force-logout" 이벤트를 보내
 * 브라우저에서 알림을 띄운 뒤 로그아웃 처리.
 */
@Slf4j
@Service
public class SseEmitterService {

    private static final long TIMEOUT = 30 * 60 * 1000L; // 30분

    // sessionId → SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 연결 등록.
     * 연결 종료(완료/타임아웃/오류) 시 맵에서 자동 제거.
     */
    public SseEmitter connect(String sessionId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> {
            emitters.remove(sessionId);
            emitter.complete();
        });
        emitter.onError(e -> emitters.remove(sessionId));

        emitters.put(sessionId, emitter);
        log.debug("SSE 연결 등록: {}", sessionId);

        // 연결 직후 더미 이벤트 (브라우저 연결 확인용)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(sessionId);
        }

        return emitter;
    }

    /**
     * 강제 로그아웃 이벤트 전송.
     * 해당 sessionId의 브라우저에 "force-logout" 이벤트를 보낸 뒤 emitter 종료.
     */
    public void sendForceLogout(String sessionId) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("force-logout")
                    .data("다른 기기에서 동일 플랫폼으로 로그인하여 이 세션이 종료됩니다."));
            emitter.complete();
        } catch (IOException e) {
            log.warn("SSE force-logout 전송 실패: {}", sessionId);
        } finally {
            emitters.remove(sessionId);
        }
    }
}
