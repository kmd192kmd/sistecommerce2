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
 * 공구 시작 전이 스케줄러 (GB-017 상태머신 중 SCHEDULED → ONGOING).
 *
 * 매분 시작 시각(start_at)이 지난 시작 전(SCHEDULED) 공구를 찾아 진행중(ONGOING)으로 전이한다.
 * 마감(GroupBuyCloseScheduler)·만료(GroupBuyExpirationScheduler)와 같은 패턴으로,
 * 조회(이 클래스)와 처리(서비스 open)를 분리하고 각 공구를 개별 트랜잭션으로 처리한다.
 *
 * 상태머신: SCHEDULED --(start_at 도래)--> ONGOING --(end_at 도래)--> CONFIRMED/FAILED
 *
 * @EnableScheduling은 ShopApplication / SchedulerConfig에 이미 활성화돼 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuyOpenScheduler {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyService groupBuyService;

    /** 매분 0초 실행. 시작 시각이 지난 예정 공구를 진행중으로 전이한다. */
    @Scheduled(cron = "0 * * * * *")
    public void openScheduledGroupBuys() {
        List<GroupBuy> targets = groupBuyRepository
                .findByStatusAndStartAtBefore(GroupBuyStatus.SCHEDULED, LocalDateTime.now());
        if (targets.isEmpty()) {
            return;
        }

        log.info("[공구 시작] 대상 {}건 처리 시작", targets.size());
        for (GroupBuy gb : targets) {
            try {
                groupBuyService.open(gb.getSeq());
            } catch (Exception e) {
                // 한 공구 실패가 나머지 시작을 막지 않도록 격리하고 로그만 남긴다 (NFR-006)
                log.error("[공구 시작] 처리 실패 groupBuySeq={}", gb.getSeq(), e);
            }
        }
    }
}
