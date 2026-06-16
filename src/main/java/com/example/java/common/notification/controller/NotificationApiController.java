package com.example.java.common.notification.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.java.common.notification.dto.NotificationResponse;
import com.example.java.common.notification.service.NotificationEmitterService;
import com.example.java.common.notification.service.NotificationService;
import com.example.java.member.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

/**
 * 회원 알림 조회/읽음 처리 REST API.
 * 회원 식별은 공구와 동일하게 SecurityContext의 CustomUserDetails(memberSeq)를 쓴다.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final NotificationEmitterService emitterService;

    /**
     * 알림 실시간 구독 (SSE). 로그인 회원의 브라우저가 페이지 로드 시 연결한다.
     * 새 알림이 생기면 "notification" 이벤트로 푸시된다.
     */
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            // 비로그인은 즉시 종료(빈 스트림). 401 본문 대신 emitter를 닫아 단순화.
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }
        return emitterService.subscribe(user.getMemberSeq());
    }

    /** 내 알림 목록 (최신순). */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.getMemberNotifications(user.getMemberSeq()));
    }

    /** 안 읽은 알림 개수 (헤더 벨 배지용). */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(user.getMemberSeq())));
    }

    /** 알림 1건 읽음 처리. */
    @PostMapping("/{seq}/read")
    public ResponseEntity<Void> read(
            @PathVariable(name = "seq") Long seq,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.markRead(user.getMemberSeq(), seq);
        return ResponseEntity.ok().build();
    }

    /** 내 알림 전체 읽음 처리. */
    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll(
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.markAllRead(user.getMemberSeq());
        return ResponseEntity.ok().build();
    }
}
