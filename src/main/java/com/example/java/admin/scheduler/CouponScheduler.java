 package com.example.java.admin.scheduler;

import com.example.java.admin.service.CouponAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduler {

    private final CouponAdminService couponAdminService;

    // cron = "초 분 시 일 월 요일" -> "0 0 0 * * *"은 매일 밤 자정(00시 00분 00초)을 의미합니다.
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleDailyCouponIssue() {
        log.info("매일 자정: 쿠폰 예약 발송 스케줄러 실행");
        try {
            couponAdminService.issueScheduledCoupons();
        } catch (Exception e) {
            log.error("스케줄러 실행 중 오류 발생", e);
        }
    }
}