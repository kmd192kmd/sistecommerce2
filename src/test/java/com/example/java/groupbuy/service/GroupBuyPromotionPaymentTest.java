package com.example.java.groupbuy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
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
import com.example.java.orders.dto.OrderCreateResultDto;
import com.example.java.orders.service.OrdersCommandService;

import org.junit.jupiter.api.BeforeEach;

/**
 * 승격자 결제(confirmPromotedPayment) 시나리오 검증.
 *
 * 결제대기(PAYMENT_PENDING) 승격자가 기한 내 결제하면 참여중(PARTICIPATING)으로 확정되고,
 * 점유 수(occupied_count)는 변하지 않아야 한다(승격 때 이미 점유).
 *
 * 결제 포트는 @MockitoBean으로 대체해 실제 PG 없이 결제 호출 횟수를 검사한다.
 * @Transactional 미사용 — 실제 commit된 상태를 검증하기 위함.
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.hikari.maximum-pool-size=10"
})
class GroupBuyPromotionPaymentTest {

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
    JdbcTemplate jdbc;

    @MockitoBean
    GroupBuyPaymentPort paymentPort;

    /** 승격자 결제 시작이 호출하는 결제대기 주문 생성(orders 도메인)은 테스트 스키마에 없으므로 모킹. */
    @MockitoBean
    OrdersCommandService ordersCommandService;

    @BeforeEach
    void stubOrderCreation() {
        when(ordersCommandService.createGroupBuyOrder(anyLong(), anyLong(), anyLong(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new OrderCreateResultDto(1L, "GB-TEST", 8000, "테스트상품"));
    }

    private static final AtomicLong SEQ = new AtomicLong(500);

    /** 테스트용 공구 1건 + 옵션 1건 직접 INSERT. @return [groupBuySeq, optionSeq] */
    private long[] createGroupBuyWithOption(LocalDateTime endAt, int orderQty, int occupied) {
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
                VALUES (?, ?, 1, ?, ?)
                """,
                optSeq, gbSeq, orderQty, occupied);
        return new long[]{gbSeq, optSeq};
    }

    /** 결제대기(PAYMENT_PENDING) 승격 participation을 직접 INSERT (결제기한 deadline 지정). */
    private void insertPaymentPending(long gbSeq, long optSeq, long memberSeq, LocalDateTime deadline) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO participation
                (seq, group_buy_seq, group_buy_options_seq, member_seq, status, payment_deadline, promoted_at, created_at)
                VALUES (participation_seq.NEXTVAL, ?, ?, ?, 'PAYMENT_PENDING', ?, ?, ?)
                """,
                gbSeq, optSeq, memberSeq, Timestamp.valueOf(deadline), Timestamp.valueOf(now), Timestamp.valueOf(now));
    }

    private ParticipationStatus statusOf(long groupBuySeq, long memberSeq) {
        List<Participation> rows = participationRepository.findByGroupBuySeqAndMemberSeq(groupBuySeq, memberSeq);
        return rows.isEmpty() ? null : rows.get(0).getStatus();
    }

    /** 대기열(waiting_queue) 행을 직접 INSERT (만료 시 승격 대상 구성용). */
    private void insertWaiting(long gbSeq, long optSeq, long memberSeq) {
        jdbc.update("""
                INSERT INTO waiting_queue (seq, group_buy_seq, group_buy_options_seq, member_seq, created_at)
                VALUES (waiting_queue_seq.NEXTVAL, ?, ?, ?, ?)
                """,
                gbSeq, optSeq, memberSeq, Timestamp.valueOf(LocalDateTime.now()));
    }

    @Test
    void 승격자가_결제시작후_결제완료되면_참여중으로_확정되고_점유는_그대로다() {
        // given: 정원 1 공구. 그 1자리는 이미 승격자가 점유(occupied=1) + 결제대기 participation
        long[] ids = createGroupBuyWithOption(LocalDateTime.now().plusDays(10), 1, 1);
        long gb = ids[0], opt = ids[1];
        insertPaymentPending(gb, opt, 9L, LocalDateTime.now().plusHours(20)); // 기한 여유
        long pSeq = participationRepository.findByGroupBuySeqAndMemberSeq(gb, 9L).get(0).getSeq();

        // when: 승격자(9번) 결제 시작 → 결제대기 주문 생성(아직 확정 아님)
        groupBuyService.startPromotedPayment(gb, 9L);

        // then: 주문 생성됨, 참여는 여전히 결제대기 (실제 결제는 토스 결제창에서)
        verify(ordersCommandService, times(1)).createGroupBuyOrder(eq(9L), eq(pSeq), anyLong(), anyInt(), anyInt(), anyInt());
        assertThat(statusOf(gb, 9L)).as("결제 시작만으론 아직 결제대기").isEqualTo(ParticipationStatus.PAYMENT_PENDING);

        // when: 토스 결제 성공 콜백 → 확정 (결제 완료 이벤트가 호출하는 메서드)
        groupBuyService.confirmAfterPayment(pSeq);

        // then: PAYMENT_PENDING → PARTICIPATING, 점유 수 변동 없음
        assertThat(statusOf(gb, 9L)).as("결제 완료로 참여 확정").isEqualTo(ParticipationStatus.PARTICIPATING);
        GroupBuyOptions after = groupBuyOptionsRepository.findById(opt).orElseThrow();
        assertThat(after.getOccupiedCount()).as("승격 때 이미 점유 → 점유 수 불변").isEqualTo(1);
    }

    @Test
    void 결제기한이_지난_승격자는_결제할_수_없다() {
        // given: 결제기한이 이미 과거인 결제대기 승격자
        long[] ids = createGroupBuyWithOption(LocalDateTime.now().plusDays(10), 1, 1);
        long gb = ids[0], opt = ids[1];
        insertPaymentPending(gb, opt, 9L, LocalDateTime.now().minusMinutes(1)); // 기한 지남

        // when/then: 결제 시작 시도 → 예외, 주문 생성 없음
        assertThatThrownBy(() -> groupBuyService.startPromotedPayment(gb, 9L))
                .as("기한 만료 결제 차단")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("결제 기한");

        assertThat(statusOf(gb, 9L)).as("여전히 결제대기 (확정 안 됨)").isEqualTo(ParticipationStatus.PAYMENT_PENDING);
        verify(ordersCommandService, times(0)).createGroupBuyOrder(anyLong(), anyLong(), anyLong(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void 결제대기_상태가_아니면_결제할_수_없다() {
        // given: 결제대기 participation이 아예 없는 공구
        long[] ids = createGroupBuyWithOption(LocalDateTime.now().plusDays(10), 1, 0);
        long gb = ids[0];

        // when/then: 결제 시작 시도 → 거부 (승격받은 적 없는 회원)
        assertThatThrownBy(() -> groupBuyService.startPromotedPayment(gb, 9L))
                .as("결제대기 대상이 없으면 거부")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("결제 대기");

        verify(ordersCommandService, times(0)).createGroupBuyOrder(anyLong(), anyLong(), anyLong(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void 결제기한_만료자는_EXPIRED되고_점유반납후_다음대기자가_승격된다() {
        // given: 정원1(이미 점유). member9가 결제대기인데 기한 지남 + 대기열에 member10
        long[] ids = createGroupBuyWithOption(LocalDateTime.now().plusDays(10), 1, 1);
        long gb = ids[0], opt = ids[1];
        insertPaymentPending(gb, opt, 9L, LocalDateTime.now().minusMinutes(1)); // 기한 지남
        insertWaiting(gb, opt, 10L);                                            // 대기열 1명

        long pSeq = participationRepository.findByGroupBuySeqAndMemberSeq(gb, 9L).get(0).getSeq();

        // when: 만료 처리 (스케줄러가 호출하는 서비스 메서드)
        groupBuyService.expirePromotion(pSeq);

        // then: 9번 EXPIRED / 10번 승격 / 점유 유지 / 환불 없음(결제 전이었으므로)
        assertThat(statusOf(gb, 9L)).as("기한 만료자는 EXPIRED").isEqualTo(ParticipationStatus.EXPIRED);
        assertThat(statusOf(gb, 10L)).as("같은 옵션 다음 대기자가 승격").isEqualTo(ParticipationStatus.PAYMENT_PENDING);

        GroupBuyOptions after = groupBuyOptionsRepository.findById(opt).orElseThrow();
        assertThat(after.getOccupiedCount()).as("점유 유지 (release -1 + 승격 +1 = 0)").isEqualTo(1);

        verify(paymentPort, times(0)).refund(anyLong()); // 결제 전이라 환불 없음
    }

    @Test
    void 결제대기자가_명시적_취소하면_CANCELLED되고_점유반납후_다음대기자가_승격된다() {
        // given: 정원1(이미 점유). member9가 결제대기(기한 여유) + 대기열에 member10
        long[] ids = createGroupBuyWithOption(LocalDateTime.now().plusDays(10), 1, 1);
        long gb = ids[0], opt = ids[1];
        insertPaymentPending(gb, opt, 9L, LocalDateTime.now().plusHours(20)); // 기한 여유 — 취소는 기한 무관
        insertWaiting(gb, opt, 10L);                                          // 대기열 1명

        long pSeq = participationRepository.findByGroupBuySeqAndMemberSeq(gb, 9L).get(0).getSeq();

        // when: 토스 결제창에서 명시적 취소 (failUrl → OrderPaymentFailedEvent → 이 메서드)
        groupBuyService.cancelPendingPayment(pSeq);

        // then: 9번 CANCELLED / 10번 승격 / 점유 유지 / 환불 없음(결제 전이었으므로)
        assertThat(statusOf(gb, 9L)).as("명시적 취소자는 CANCELLED").isEqualTo(ParticipationStatus.CANCELLED);
        assertThat(statusOf(gb, 10L)).as("같은 옵션 다음 대기자가 승격").isEqualTo(ParticipationStatus.PAYMENT_PENDING);

        GroupBuyOptions after = groupBuyOptionsRepository.findById(opt).orElseThrow();
        assertThat(after.getOccupiedCount()).as("점유 유지 (release -1 + 승격 +1 = 0)").isEqualTo(1);

        verify(paymentPort, times(0)).refund(anyLong()); // 결제 전이라 환불 없음
    }

    @Test
    void 이미_결제완료된_참여는_취소콜백이_와도_변화없다_멱등() {
        // given: 결제대기였다가 결제 완료(PARTICIPATING)된 참여 — 취소 대상이 아니어야
        long[] ids = createGroupBuyWithOption(LocalDateTime.now().plusDays(10), 1, 1);
        long gb = ids[0], opt = ids[1];
        insertPaymentPending(gb, opt, 9L, LocalDateTime.now().plusHours(20));
        long pSeq = participationRepository.findByGroupBuySeqAndMemberSeq(gb, 9L).get(0).getSeq();
        groupBuyService.confirmAfterPayment(pSeq); // 결제 완료 → PARTICIPATING

        // when: 뒤늦은(중복) failUrl 콜백으로 취소 시도
        groupBuyService.cancelPendingPayment(pSeq);

        // then: PARTICIPATING 유지(취소 안 됨), 점유 불변 — PAYMENT_PENDING이 아니면 멱등 무시
        assertThat(statusOf(gb, 9L)).as("결제완료자는 취소 콜백에도 PARTICIPATING 유지")
                .isEqualTo(ParticipationStatus.PARTICIPATING);
        GroupBuyOptions after = groupBuyOptionsRepository.findById(opt).orElseThrow();
        assertThat(after.getOccupiedCount()).as("점유 불변").isEqualTo(1);
    }
}
