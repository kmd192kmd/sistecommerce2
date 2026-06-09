package com.example.java.groupbuy.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.java.groupbuy.entity.Participation;
import com.example.java.groupbuy.entity.ParticipationStatus;
import com.example.java.groupbuy.repository.ParticipationRepository;
import com.example.java.groupbuy.service.GroupBuyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 승격자 결제기한 만료 처리 스케줄러 (GB-023).
 *
 * 매분 결제기한(payment_deadline)이 지난 결제대기(PAYMENT_PENDING) 승격자를 찾아
 * 한 건씩 만료 처리한다. 만료 시 자리(점유)를 반납하고 같은 옵션의 다음 대기자를 승격시킨다.
 *
 * 설계 메모:
 *  - 조회(이 클래스)와 처리(서비스 expirePromotion)를 분리한다. 각 건을 개별 트랜잭션으로 처리해
 *    한 건이 실패해도 나머지 건 처리가 막히지 않게 한다 (try-catch로 격리).
 *  - 만료자는 대기열에 자동 재등록하지 않는다 (자격 소멸. 재참여는 사용자가 직접 신청 — GB-023 정책).
 *  - @EnableScheduling은 ShopApplication / SchedulerConfig에 이미 활성화돼 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuyExpirationScheduler {

    private final ParticipationRepository participationRepository;
    private final GroupBuyService groupBuyService;

    /** 매분 0초 실행. 결제기한이 분 단위라 1분 주기로 충분하다. */
    @Scheduled(cron = "0 * * * * *")
    public void expirePromotedPayments() {
        LocalDateTime now = LocalDateTime.now();

        // 기한 지난 결제대기 승격자 조회 (조회는 트랜잭션 밖 — seq만 꺼내 개별 처리에 넘긴다)
        List<Participation> expired = participationRepository
                .findByStatusAndPaymentDeadlineBefore(ParticipationStatus.PAYMENT_PENDING, now);
        if (expired.isEmpty()) {
            return;
        }

        log.info("[공구 결제기한 만료] 대상 {}건 처리 시작", expired.size());
        for (Participation p : expired) {
            try {
                // 실제 만료·점유복구·승격은 서비스의 개별 트랜잭션에서 (락·재검증 포함)
                groupBuyService.expirePromotion(p.getSeq());
            } catch (Exception e) {
                // 한 건 실패가 나머지 처리를 막지 않도록 격리하고 로그만 남긴다 (NFR-006 관찰가능성)
                log.error("[공구 결제기한 만료] 처리 실패 participationSeq={}", p.getSeq(), e);
            }
        }
    }
}
