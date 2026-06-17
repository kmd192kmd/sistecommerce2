package com.example.java.member.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.repository.RememberMeTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 자동로그인 토큰 정리 스케줄러.
 *
 * 매일 새벽 03:30 — 만료(expire_at 경과)되었거나 이미 사용된(used_yn='Y') 토큰을 삭제한다.
 * 1회용 회전 정책상 used 토큰이 계속 쌓이므로 주기적 정리가 필요하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RememberMeTokenScheduler {

    private final RememberMeTokenRepository rememberMeTokenRepository;

    @Transactional
    @Scheduled(cron = "0 30 3 * * *")   // 매일 03:30
    public void cleanUp() {
        int deleted = rememberMeTokenRepository.deleteExpiredOrUsed(LocalDateTime.now());
        log.info("[자동로그인 토큰 스케줄러] 만료/사용 토큰 정리: {}건", deleted);
    }
}
