package com.example.java.groupbuy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.entity.Participation;
import com.example.java.groupbuy.entity.ParticipationStatus;
import com.example.java.groupbuy.payment.GroupBuyPaymentPort;
import com.example.java.groupbuy.repository.GroupBuyOptionsRepository;
import com.example.java.groupbuy.repository.ParticipationRepository;
import com.example.java.groupbuy.repository.WaitingQueueRepository;
import com.example.java.orders.dto.OrderCreateResultDto;
import com.example.java.orders.service.OrdersCommandService;
import com.example.java.common.notification.entity.Notification;
import com.example.java.common.notification.entity.NotificationRecipientType;
import com.example.java.common.notification.entity.NotificationType;
import com.example.java.common.notification.repository.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;

/**
 * 공구 취소 + 점유 복구 + 대기열 FIFO 승격 시나리오 검증.
 *
 * 격리된 Oracle XE(Testcontainers)에서, 각 테스트가 필요한 공구/옵션을 직접 만들고
 * 실제 participate()/cancel() 흐름으로 상태를 구성해 검증한다.
 *
 * 결제 포트는 @MockitoBean으로 대체한다 — 실제 PG 호출 없이 동작하며,
 * 환불(cancel) 호출 횟수를 verify로 검사해 멱등성(NFR-004)을 확인하기 위함이다.
 *
 * 주의: @Transactional 을 붙이지 않는다 — 멱등성 테스트가 여러 스레드에서 실제 commit된
 * 상태를 봐야 하기 때문(테스트 롤백을 쓰면 동시성이 성립 안 됨).
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.hikari.maximum-pool-size=10"
})
class GroupBuyCancelTest {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            .withInitScript("groupbuy-lock-schema.sql");

    @Autowired
    GroupBuyService groupBuyService;

    @Autowired
    GroupBuyOptionsRepository groupBuyOptionsRepository;

    @Autowired
    ParticipationRepository participationRepository;

    @Autowired
    WaitingQueueRepository waitingQueueRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    NotificationRepository notificationRepository;

    /** 환불 횟수를 검증하려고 결제 포트를 목으로 대체(refund가 아무 동작 안 함). */
    @MockitoBean
    GroupBuyPaymentPort paymentPort;

    /** participate가 호출하는 결제대기 주문 생성(orders 도메인)은 테스트 스키마에 없으므로 모킹. */
    @MockitoBean
    OrdersCommandService ordersCommandService;

    @BeforeEach
    void stubOrderCreation() {
        when(ordersCommandService.createGroupBuyOrder(anyLong(), anyLong(), anyLong(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new OrderCreateResultDto(1L, "GB-TEST", 8000, "테스트상품"));
        // 알림은 member_seq 단위로 누적된다(공구는 테스트마다 새로 만들지만 회원 1·2·3은 재사용).
        // 테스트 간 격리를 위해 매 테스트 전에 알림 테이블을 비운다.
        notificationRepository.deleteAll();
    }

    /** 동적 생성하는 공구/옵션 seq 발급기 (스키마 더미 seq=1과 안 겹치게 100부터). */
    private static final AtomicLong SEQ = new AtomicLong(100);

    /**
     * 테스트용 공구 1건 + 옵션 1건을 직접 INSERT 한다.
     * product/options FK는 최소 스키마에 없으므로 컬럼 값(1)만 채운다.
     *
     * @param endAt    공구 마감 시각 (T_lock 검증용으로 테스트마다 다르게)
     * @param orderQty 옵션 발주 가능 수량 (= 정원)
     * @return [groupBuySeq, optionSeq]
     */
    private long[] createGroupBuyWithOption(LocalDateTime endAt, int orderQty) {
        long gbSeq = SEQ.getAndIncrement();
        long optSeq = SEQ.getAndIncrement();
        LocalDateTime past = LocalDateTime.of(2000, 1, 1, 0, 0);
        jdbc.update("""
                INSERT INTO group_buy
                (seq, product_seq, start_at, end_at, created_at, min_count, max_count, original_price, final_price, status)
                VALUES (?, 1, ?, ?, ?, 1, ?, 10000, 8000, 'ONGOING')
                """,
                gbSeq, Timestamp.valueOf(past), Timestamp.valueOf(endAt), Timestamp.valueOf(past), orderQty);
        jdbc.update("""
                INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
                VALUES (?, ?, 1, ?, 0)
                """,
                optSeq, gbSeq, orderQty);
        return new long[]{gbSeq, optSeq};
    }

    /**
     * 참여 신청(결제대기) → 결제 완료까지 시뮬레이션해서 PARTICIPATING으로 만든다.
     * 토스 2단계 흐름상 participate는 PAYMENT_PENDING까지만 만들므로, 취소 대상(결제 완료자)을
     * 만들려면 결제 성공 이벤트가 호출하는 confirmAfterPayment까지 태워야 한다.
     */
    private void joinPaid(long groupBuySeq, long optionSeq, long memberSeq) {
        groupBuyService.participate(groupBuySeq, optionSeq, memberSeq);
        long participationSeq = participationRepository
                .findByGroupBuySeqAndMemberSeq(groupBuySeq, memberSeq).get(0).getSeq();
        groupBuyService.confirmAfterPayment(participationSeq);
    }

    /** 특정 공구에서 해당 회원의 참여 상태를 꺼낸다 (없으면 null). */
    private ParticipationStatus statusOf(long groupBuySeq, long memberSeq) {
        List<Participation> rows = participationRepository.findByGroupBuySeqAndMemberSeq(groupBuySeq, memberSeq);
        return rows.isEmpty() ? null : rows.get(0).getStatus();
    }

    @Test
    void 취소하면_점유복구되고_같은옵션_대기자가_승격된다() {
        // given: 정원 2 공구. 2명 정규참여로 매진 + 1명 대기열
        long[] ids = createGroupBuyWithOption(LocalDateTime.now().plusDays(10), 2);
        long gb = ids[0], opt = ids[1];
        joinPaid(gb, opt, 1L);                    // 결제 완료(취소 대상)
        groupBuyService.participate(gb, opt, 2L); // 2번째 정원 채움(결제대기)
        groupBuyService.participate(gb, opt, 3L); // 매진이라 대기열로

        // when: 1번 회원이 취소
        groupBuyService.cancel(gb, 1L);

        // then: 취소자는 CANCELLED, 대기자(3번)는 PAYMENT_PENDING으로 승격
        assertThat(statusOf(gb, 1L)).as("취소자는 CANCELLED").isEqualTo(ParticipationStatus.CANCELLED);

        List<Participation> promoted = participationRepository.findByGroupBuySeqAndMemberSeq(gb, 3L);
        assertThat(promoted).as("대기자가 승격되어 participation 생성됨").hasSize(1);
        assertThat(promoted.get(0).getStatus()).as("승격자는 결제대기").isEqualTo(ParticipationStatus.PAYMENT_PENDING);
        assertThat(promoted.get(0).getPaymentDeadline()).as("승격자 결제기한 설정됨").isNotNull();
        assertThat(promoted.get(0).getPromotedAt()).as("승격 시각 설정됨").isNotNull();

        // 알림 검증
        // 3번(승격된 회원): 승격 알림 1건
        assertThat(notificationRepository.countByRecipientTypeAndRecipientSeqAndReadAtIsNull(
                NotificationRecipientType.MEMBER, 3L))
                .as("승격된 회원에게 읽지 않은 알림 1건 생성").isEqualTo(1);

        // 1번(결제완료 후 취소): 결제완료(PAYMENT_DONE) + 환불완료(REFUND_DONE) 2건
        List<Notification> notifications1 = notificationRepository
                .findByRecipientTypeAndRecipientSeqOrderByCreatedAtDesc(NotificationRecipientType.MEMBER, 1L);
        assertThat(notifications1).hasSize(2);
        assertThat(notifications1.get(0).getType()).isEqualTo(NotificationType.REFUND_DONE);
        assertThat(notifications1.get(1).getType()).isEqualTo(NotificationType.PAYMENT_DONE);

        // 2번(결제대기 상태 유지): 알림 없음
        assertThat(notificationRepository
                .findByRecipientTypeAndRecipientSeqOrderByCreatedAtDesc(NotificationRecipientType.MEMBER, 2L))
                .isEmpty();

        GroupBuyOptions after = groupBuyOptionsRepository.findById(opt).orElseThrow();
        assertThat(after.getOccupiedCount()).as("점유 수 유지 (release -1 후 승격 occupy +1 = net 0)").isEqualTo(2);

        assertThat(waitingQueueRepository.findFirstByGroupBuyOptionsOrderByCreatedAtAsc(after))
                .as("대기열은 승격으로 비워짐").isEmpty();
    }

    @Test
    void 대기자가_여럿이면_가장_먼저_등록한_사람이_승격된다() {
        // given: 정원 1 공구. 1명 매진 + 2명 대기열(2번이 3번보다 먼저 등록)
        long[] ids = createGroupBuyWithOption(LocalDateTime.now().plusDays(10), 1);
        long gb = ids[0], opt = ids[1];
        joinPaid(gb, opt, 1L);                    // 결제 완료(정원 채움, 취소 대상)
        groupBuyService.participate(gb, opt, 2L); // 대기열 1순위 (먼저)
        groupBuyService.participate(gb, opt, 3L); // 대기열 2순위 (나중)

        // when: 정규참여자(1번) 취소 → 자리 1개
        groupBuyService.cancel(gb, 1L);

        // then: FIFO라 먼저 등록한 2번이 승격, 3번은 아직 대기 (NFR-002)
        assertThat(statusOf(gb, 2L)).as("먼저 기다린 2번이 승격").isEqualTo(ParticipationStatus.PAYMENT_PENDING);
        assertThat(statusOf(gb, 3L)).as("나중에 온 3번은 아직 미승격(participation 없음)").isNull();

        GroupBuyOptions after = groupBuyOptionsRepository.findById(opt).orElseThrow();
        assertThat(waitingQueueRepository.findFirstByGroupBuyOptionsOrderByCreatedAtAsc(after).orElseThrow().getMemberSeq())
                .as("대기열엔 3번만 남음").isEqualTo(3L);
    }

    @Test
    void 마감_24시간_이내에는_취소할_수_없다() {
        // given: 마감이 10시간 뒤인 공구 (T_lock 24h 구간 안)
        long[] ids = createGroupBuyWithOption(LocalDateTime.now().plusHours(10), 2);
        long gb = ids[0], opt = ids[1];
        joinPaid(gb, opt, 1L);

        // when/then: 취소 시도 → 예외
        assertThatThrownBy(() -> groupBuyService.cancel(gb, 1L))
                .as("마감 임박 구간 취소 차단")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("안내: 마감 24시간 이내에는 취소할 수 없습니다.");

        // 상태는 그대로여야 한다 (취소도 환불도 일어나지 않음)
        assertThat(statusOf(gb, 1L)).as("여전히 참여 중").isEqualTo(ParticipationStatus.PARTICIPATING);
        GroupBuyOptions after = groupBuyOptionsRepository.findById(opt).orElseThrow();
        assertThat(after.getOccupiedCount()).as("점유 수 변동 없음").isEqualTo(1);
    }

    @Test
    void 같은_취소요청이_동시에_와도_환불은_한_번만_일어난다() throws InterruptedException {
        // given: 정원 2 매진 + 대기자 1
        long[] ids = createGroupBuyWithOption(LocalDateTime.now().plusDays(10), 2);
        long gb = ids[0], opt = ids[1];
        joinPaid(gb, opt, 1L);                    // 결제 완료(취소 대상)
        groupBuyService.participate(gb, opt, 2L); // 2번째 정원 채움(결제대기)
        groupBuyService.participate(gb, opt, 3L); // 대기열

        // when: 1번 회원의 cancel 을 2개 스레드가 동시에 호출
        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    groupBuyService.cancel(gb, 1L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException ignored) {
                    // 한쪽이 "취소할 공구가 없음" 등으로 실패할 수 있으나, 
                	// 환불 1회만 검증하면 충분
                }
            });
        }
        ready.await();
        start.countDown();
        pool.shutdown();
        boolean finished = pool.awaitTermination(60, TimeUnit.SECONDS);

        // then
        assertThat(finished).as("모든 스레드 종료").isTrue();
        verify(paymentPort, times(1)).refund(anyLong()); // 환불은 정확히 1회 (NFR-004)
        assertThat(statusOf(gb, 1L)).as("취소자는 CANCELLED").isEqualTo(ParticipationStatus.CANCELLED);
        assertThat(statusOf(gb, 3L)).as("승격은 한 번만 (대기자 1명 승격)").isEqualTo(ParticipationStatus.PAYMENT_PENDING);

        GroupBuyOptions after = groupBuyOptionsRepository.findById(opt).orElseThrow();
        assertThat(after.getOccupiedCount()).as("점유 수 유지 (중복 취소가 점유를 두 번 건드리지 않음)").isEqualTo(2);
    }
}
