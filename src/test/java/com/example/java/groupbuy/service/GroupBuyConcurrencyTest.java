package com.example.java.groupbuy.service;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.repository.GroupBuyOptionsRepository;

/**
 * 비관적 락 동시성 검증.
 *
 * 격리된 Oracle XE(Testcontainers)에 order_qty=10 옵션 1건을 두고,
 * 30개 스레드가 동시에 participate()로 점유를 시도한다.
 * 락이 정상 작동하면 정확히 10건만 성공하고 occupied_count는 10을 넘지 않는다 (NFR-001).
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

    private static final Long OPTION_SEQ = 1L;
    private static final int ORDER_QTY = 10;
    private static final int THREADS = 30;

    @Autowired
    GroupBuyService groupBuyService;

    @Autowired
    GroupBuyOptionsRepository groupBuyOptionsRepository;

    @Test
    void 동시참여_정원초과없이_정확히_orderQty만큼만_점유된다() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS); // 전 스레드 준비 완료 신호
        CountDownLatch start = new CountDownLatch(1);        // 동시 출발 신호
        AtomicInteger success = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    groupBuyService.participate(OPTION_SEQ);
                    success.incrementAndGet();
                } catch (IllegalStateException e) {
                    // occupy()가 매진 시 던지는 예외 = 정상적인 '자리 없음'
                    soldOut.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();          // 모두 출발선에 설 때까지 대기
        start.countDown();      // 동시 출발!
        pool.shutdown();
        boolean finished = pool.awaitTermination(60, TimeUnit.SECONDS);

        assertThat(finished).as("모든 스레드가 제한시간 내 종료").isTrue();
        assertThat(success.get()).as("성공 = 발주 가능 수량").isEqualTo(ORDER_QTY);
        assertThat(soldOut.get()).as("나머지는 매진 처리").isEqualTo(THREADS - ORDER_QTY);

        GroupBuyOptions after = groupBuyOptionsRepository.findById(OPTION_SEQ).orElseThrow();
        assertThat(after.getOccupiedCount())
                .as("점유 수가 발주 가능 수량을 절대 초과하지 않음 (NFR-001)")
                .isEqualTo(ORDER_QTY);
    }
}
