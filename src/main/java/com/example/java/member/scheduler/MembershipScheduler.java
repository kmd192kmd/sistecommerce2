package com.example.java.member.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.entity.Memberships;
import com.example.java.member.repository.MembershipsRepository;
import com.example.java.member.service.MembershipService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 멤버십 자동 처리 스케줄러.
 *
 * - 만료 처리 : 매일 새벽 02:00 — 만료일이 지난 canceled 멤버십을 expired 로 전환
 * - 자동 갱신 : 매일 오전 09:00 — nextBillingAt이 지난 active 멤버십을 Toss 빌링으로 갱신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipScheduler {

    private final MembershipsRepository membershipsRepository;
    private final MembershipService     membershipService;

    /**
     * 만료 처리.
     * canceled 상태이면서 expireAt이 현재 시각 이전인 멤버십 → expired 로 변경.
     */
    @Transactional
    @Scheduled(cron = "0 0 2 * * *")   // 매일 02:00
    public void processExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<Memberships> targets = membershipsRepository
                .findByStatusAndExpireAtBefore(Memberships.STATUS_CANCELED, now);

        log.info("[멤버십 스케줄러] 만료 처리 대상: {}건", targets.size());

        for (Memberships m : targets) {
            try {
                membershipService.expire(m);
            } catch (Exception e) {
                log.error("[멤버십 스케줄러] 만료 처리 실패 - membershipSeq: {}, error: {}",
                        m.getSeq(), e.getMessage());
            }
        }
    }

    /**
     * 자동 갱신.
     * active 상태이면서 nextBillingAt이 현재 시각 이전인 멤버십 → Toss 빌링 후 +1개월 갱신.
     */
    @Transactional
    @Scheduled(cron = "0 0 9 * * *")   // 매일 09:00
    public void processRenewal() {
        LocalDateTime now = LocalDateTime.now();
        List<Memberships> targets = membershipsRepository
                .findByStatusAndNextBillingAtBefore(Memberships.STATUS_ACTIVE, now);

        log.info("[멤버십 스케줄러] 자동 갱신 대상: {}건", targets.size());

        for (Memberships m : targets) {
            try {
                membershipService.renew(m);
            } catch (Exception e) {
                log.error("[멤버십 스케줄러] 자동 갱신 실패 - membershipSeq: {}, error: {}",
                        m.getSeq(), e.getMessage());
            }
        }
    }
}
