package com.example.java.groupbuy.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyStatus;
import com.example.java.groupbuy.repository.GroupBuyRepository;
import com.example.java.groupbuy.service.GroupBuyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 공구 마감 자동 처리 스케줄러 (GB-025).
 *
 * 매분 마감 시각(end_at)이 지난 진행 중(ONGOING) 공구를 찾아 한 건씩 확정/무산 판정한다.
 * 인원과 무관하게 마감 시각이 도래하면 자동 마감하고, 확정 인원이 최소 인원 이상이면
 * 확정(CONFIRMED), 미만이면 무산(FAILED, 전원 환불)으로 처리한다.
 *
 * 설계 메모:
 *  - 조회(이 클래스)와 처리(서비스 close)를 분리한다. 각 공구를 개별 트랜잭션으로 처리해
 *    한 공구의 실패가 다른 공구 처리를 막지 않게 한다 (try-catch 격리).
 *  - 확정 후 자동 발주(GROP-006)는 관리자/발주 영역이라 여기서는 판정·상태 전이까지만 한다.
 *  - @EnableScheduling은 ShopApplication / SchedulerConfig에 이미 활성화돼 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuyCloseScheduler {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyService groupBuyService;

    /** 매분 0초 실행. 마감 시각이 지난 진행 중 공구를 확정/무산 처리한다. */
    @Scheduled(cron = "0 * * * * *")
    public void closeExpiredGroupBuys() {
        List<GroupBuy> targets = groupBuyRepository
                .findByStatusAndEndAtBefore(GroupBuyStatus.ONGOING, LocalDateTime.now());
        if (targets.isEmpty()) {
            return;
        }

        log.info("[공구 마감] 대상 {}건 처리 시작", targets.size());
        for (GroupBuy gb : targets) {
            try {
                groupBuyService.close(gb.getSeq());
            } catch (Exception e) {
                // 한 공구 실패가 나머지 마감을 막지 않도록 격리하고 로그만 남긴다 (NFR-006)
                log.error("[공구 마감] 처리 실패 groupBuySeq={}", gb.getSeq(), e);
            }
        }
    }
}
