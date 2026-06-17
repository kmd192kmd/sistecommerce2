package com.example.java.groupbuy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.java.groupbuy.dto.ParticipateResponse;
import com.example.java.groupbuy.dto.ParticipateResult;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.repository.GroupBuyOptionsRepository;
import com.example.java.groupbuy.repository.ParticipationRepository;
import com.example.java.groupbuy.repository.WaitingQueueRepository;
import com.example.java.orders.dto.OrderCreateResultDto;
import com.example.java.orders.service.OrdersCommandService;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 공구 참여 동시성 검증 (점유 + participation INSERT + 매진 시 대기열 분기 전체 흐름).
 *
 * 격리된 Oracle XE(Testcontainers)에 ONGOING 공구 1건 + order_qty=10 옵션 1건을 두고,
 * 30개 스레드가 서로 다른 회원으로 동시에 participate()를 시도한다.
 * 락이 정상 작동하면:
 *  - 정확히 10명만 정규 참여(PARTICIPATED) → occupied_count·participation 모두 10 (정원 초과 없음, NFR-001)
 *  - 나머지 20명은 매진이라 대기열 등록(QUEUED) → waiting_queue 20행
 *
 * 주의: @Transactional 을 클래스에 붙이지 않는다 — 각 스레드의 participate()가 실제로
 * commit 되어야 다른 스레드가 그 점유 결과를 보기 때문(테스트 롤백을 쓰면 동시성이 성립 안 됨).
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        // 커넥션 부족이 아니라 '락'이 직렬화하는 걸 보려고 풀을 스레드 수만큼 키운다.
        "spring.datasource.hikari.maximum-pool-size=30"
})
class GroupBuyConcurrencyTest {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            .withInitScript("groupbuy-lock-schema.sql");

    private static final Long GROUP_BUY_SEQ = 1L;
    private static final Long OPTION_SEQ = 1L;
    private static final int ORDER_QTY = 10;
    private static final int THREADS = 30;

    @Autowired
    GroupBuyService groupBuyService;

    @Autowired
    GroupBuyOptionsRepository groupBuyOptionsRepository;

    @Autowired
    ParticipationRepository participationRepository;

    @Autowired
    WaitingQueueRepository waitingQueueRepository;

    // 결제 대기 주문 생성(orders 도메인)은 이 테스트의 관심사(점유 동시성)가 아니고
    // 테스트 스키마엔 orders 테이블이 없으므로 모킹한다. participate는 반환값만 사용한다.
    @MockitoBean
    OrdersCommandService ordersCommandService;

    @Test
    void 동시참여_정원은_정규참여_나머지는_대기열로_분기된다() throws InterruptedException {
        when(ordersCommandService.createGroupBuyOrder(anyLong(), anyLong(), anyLong(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new OrderCreateResultDto(1L, "GB-TEST", 0, "테스트상품"));

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS); // 전 스레드 준비 완료 신호
        CountDownLatch start = new CountDownLatch(1);        // 동시 출발 신호
        AtomicInteger participated = new AtomicInteger();    // 정규 참여(PARTICIPATED)
        AtomicInteger queued = new AtomicInteger();          // 대기열 등록(QUEUED)
        AtomicInteger failed = new AtomicInteger();          // 예상치 못한 예외 (0이어야 정상)

        for (int i = 0; i < THREADS; i++) {
            final long memberSeq = i + 1; // 30명 서로 다른 회원 (같은 회원이면 중복참여로 걸러짐)
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    // 매진은 더 이상 예외가 아니라 QUEUED 반환이므로, 반환값으로 결과를 분류한다.
                    ParticipateResponse response = groupBuyService.participate(GROUP_BUY_SEQ, OPTION_SEQ, memberSeq);
                    if (response.result() == ParticipateResult.PARTICIPATED) {
                        participated.incrementAndGet();
                    } else {
                        queued.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    // 정상 흐름(참여/대기)에선 예외가 없어야 한다. 여기로 오면 실패로 드러나야 하므로 센다.
                    failed.incrementAndGet();
                }
            });
        }

        ready.await();          // 모두 출발선에 설 때까지 대기
        start.countDown();      // 동시 출발!
        pool.shutdown();
        boolean finished = pool.awaitTermination(60, TimeUnit.SECONDS);

        assertThat(finished).as("모든 스레드가 제한시간 내 종료").isTrue();
        assertThat(failed.get()).as("예상치 못한 예외 없음").isZero();
        assertThat(participated.get()).as("정규 참여 = 발주 가능 수량").isEqualTo(ORDER_QTY);
        assertThat(queued.get()).as("나머지는 매진이라 대기열 등록").isEqualTo(THREADS - ORDER_QTY);

        GroupBuyOptions after = groupBuyOptionsRepository.findById(OPTION_SEQ).orElseThrow();
        assertThat(after.getOccupiedCount())
                .as("점유 수가 발주 가능 수량을 절대 초과하지 않음 (NFR-001)")
                .isEqualTo(ORDER_QTY);

        assertThat(participationRepository.findByGroupBuySeq(GROUP_BUY_SEQ))
                .as("정규 참여(participation) 건수 = 발주 가능 수량 (점유 = INSERT 정합성)")
                .hasSize(ORDER_QTY);

        assertThat(waitingQueueRepository.count())
                .as("매진으로 밀린 인원은 모두 대기열(waiting_queue)에 등록됨")
                .isEqualTo(THREADS - ORDER_QTY);
    }
}
