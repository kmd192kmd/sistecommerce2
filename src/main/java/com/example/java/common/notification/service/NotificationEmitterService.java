package com.example.java.common.notification.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.java.common.notification.dto.NotificationResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 회원별 알림 SSE 연결 관리 + 실시간 푸시.
 *
 * 기존 member.SseEmitterService는 sessionId 기준(강제 로그아웃 전용)이라 재사용하지 않고,
 * 알림은 회원(memberSeq) 단위로 푸시해야 하므로 별도 레지스트리를 둔다.
 * 한 회원이 여러 탭을 열 수 있으므로 memberSeq → emitter 리스트로 관리한다.
 *
 * 푸시는 best-effort다 — 전송이 실패해도(연결 끊김 등) 알림은 이미 DB에 저장돼 있어
 * 다음 조회 시 보이므로, 예외를 비즈 로직으로 전파하지 않는다.
 */
@Slf4j
@Service
public class NotificationEmitterService {

    private static final long TIMEOUT = 30 * 60 * 1000L; // 30분

    // memberSeq → 그 회원의 열린 SSE 연결들(여러 탭)
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** 구독 등록. 연결 종료(완료/타임아웃/오류) 시 리스트에서 자동 제거. */
    public SseEmitter subscribe(Long memberSeq) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        emitters.computeIfAbsent(memberSeq, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(memberSeq, emitter));
        emitter.onTimeout(() -> {
            remove(memberSeq, emitter);
            emitter.complete();
        });
        emitter.onError(e -> remove(memberSeq, emitter));

        // 연결 확인용 더미 이벤트 (프록시 버퍼링 방지 겸용)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            remove(memberSeq, emitter);
        }
        return emitter;
    }

    /** 해당 회원의 모든 열린 연결로 알림 1건 푸시 (best-effort). */
    public void push(Long memberSeq, NotificationResponse notification) {
        List<SseEmitter> targets = emitters.get(memberSeq);
        if (targets == null || targets.isEmpty()) {
            return; // 열린 연결 없음 — 다음 조회 때 보임
        }
        for (SseEmitter emitter : targets) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (Exception e) {
                remove(memberSeq, emitter);
            }
        }
    }

    private void remove(Long memberSeq, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(memberSeq);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(memberSeq);
            }
        }
    }
}
