package com.example.java.member.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.java.member.service.SseEmitterService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    /**
     * SSE 구독 엔드포인트.
     * 로그인된 사용자의 브라우저가 최초 로드 시 연결.
     * sessionId를 키로 emitter 등록.
     */
    @GetMapping(value = "/connect", produces = "text/event-stream")
    public SseEmitter connect(HttpSession session) {
        return sseEmitterService.connect(session.getId());
    }
}
