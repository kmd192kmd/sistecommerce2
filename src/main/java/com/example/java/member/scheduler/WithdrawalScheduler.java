package com.example.java.member.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.java.member.service.WithdrawalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 탈퇴 처리 스케줄러.
 *
 * 확정 마스킹 — 매일 04:00. 유예(3일) 경과한 보류 탈퇴 건을 마스킹·확정한다(status 4→5).
 * 최종 파기 — 매일 04:30. 보존(5년) 경과한 확정 건의 분리보관 원본을 비가역 파기한다.
 * 각 건은 WithdrawalService 에서 개별 트랜잭션으로 처리되어, 한 건 실패가 전체를 롤백하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalScheduler {

    private final WithdrawalService withdrawalService;

    @Scheduled(cron = "0 0 4 * * *")   // 매일 04:00
    public void confirmExpiredWithdrawals() {
        List<Long> targets = withdrawalService.findExpiredPendingSeqs(LocalDate.now());
        log.info("[탈퇴 확정 스케줄러] 대상: {}건", targets.size());

        int success = 0;
        for (Long withdrawalSeq : targets) {
            try {
                withdrawalService.confirm(withdrawalSeq);
                success++;
            } catch (Exception e) {
                log.error("[탈퇴 확정 스케줄러] 실패 - withdrawalSeq: {}, error: {}",
                        withdrawalSeq, e.getMessage());
            }
        }
        log.info("[탈퇴 확정 스케줄러] 완료: {}/{}건", success, targets.size());
    }

    @Scheduled(cron = "0 30 4 * * *")   // 매일 04:30
    public void finalPurgeWithdrawals() {
        List<Long> targets = withdrawalService.findFinalPurgeSeqs(LocalDate.now());
        log.info("[탈퇴 최종파기 스케줄러] 대상: {}건", targets.size());

        int success = 0;
        for (Long withdrawalSeq : targets) {
            try {
                withdrawalService.purge(withdrawalSeq);
                success++;
            } catch (Exception e) {
                log.error("[탈퇴 최종파기 스케줄러] 실패 - withdrawalSeq: {}, error: {}",
                        withdrawalSeq, e.getMessage());
            }
        }
        log.info("[탈퇴 최종파기 스케줄러] 완료: {}/{}건", success, targets.size());
    }
}
