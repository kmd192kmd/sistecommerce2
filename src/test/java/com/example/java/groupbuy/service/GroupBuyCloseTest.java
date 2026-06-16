package com.example.java.groupbuy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

import com.example.java.groupbuy.entity.GroupBuyStatus;
import com.example.java.groupbuy.entity.Participation;
import com.example.java.groupbuy.entity.ParticipationStatus;
import com.example.java.groupbuy.payment.GroupBuyPaymentPort;
import com.example.java.groupbuy.repository.GroupBuyRepository;
import com.example.java.groupbuy.repository.ParticipationRepository;
import com.example.java.common.notification.entity.Notification;
import com.example.java.common.notification.entity.NotificationRecipientType;
import com.example.java.common.notification.entity.NotificationType;
import com.example.java.common.notification.repository.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;

/**
 * 공구 마감 확정/무산 판정(close) 시나리오 검증.
 *
 * 마감은 end_at이 과거여야 동작하므로 participate()로는 상태를 못 만든다(마감된 공구는 참여 거부).
 * 그래서 공구와 participation을 JdbcTemplate으로 직접 구성한 뒤 close()를 호출해 검증한다.
 *
 * 결제 포트는 @MockitoBean으로 대체해 무산 시 환불(cancel) 호출 횟수를 검사한다.
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.hikari.maximum-pool-size=10"
})
class GroupBuyCloseTest {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            .withInitScript("groupbuy-lock-schema.sql");

    @Autowired
    GroupBuyService groupBuyService;

    @Autowired
    GroupBuyRepository groupBuyRepository;

    @Autowired
    ParticipationRepository participationRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    NotificationRepository notificationRepository;

    @MockitoBean
    GroupBuyPaymentPort paymentPort;

    /** 알림은 member_seq 단위로 누적된다(회원 1·2·3 재사용). 테스트 간 격리를 위해 매번 비운다. */
    @BeforeEach
    void clearNotifications() {
        notificationRepository.deleteAll();
    }

    private static final AtomicLong SEQ = new AtomicLong(700);

    /**
     * 이미 마감 시각이 지난(end_at = 과거) ONGOING 공구 1건 + 옵션 1건을 직접 INSERT.
     * @return [groupBuySeq, optionSeq]
     */
    private long[] createClosableGroupBuy(int minCount) {
        long gbSeq = SEQ.getAndIncrement();
        long optSeq = SEQ.getAndIncrement();
        LocalDateTime past = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime endPast = LocalDateTime.now().minusMinutes(1); // 마감 시각 지남
        jdbc.update("""
                INSERT INTO group_buy
                (seq, product_seq, start_at, end_at, created_at, min_count, max_count, original_price, final_price, status)
                VALUES (?, 1, ?, ?, ?, ?, 100, 10000, 8000, 'ONGOING')
                """,
                gbSeq, Timestamp.valueOf(past), Timestamp.valueOf(endPast), Timestamp.valueOf(past), minCount);
        jdbc.update("""
                INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
                VALUES (?, ?, 1, 100, 0)
                """,
                optSeq, gbSeq);
        return new long[]{gbSeq, optSeq};
    }

    /** participation 한 건 직접 INSERT (원하는 상태로). */
    private void insertParticipation(long gbSeq, long optSeq, long memberSeq, String status) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO participation
                (seq, group_buy_seq, group_buy_options_seq, member_seq, status, created_at)
                VALUES (participation_seq.NEXTVAL, ?, ?, ?, ?, ?)
                """,
                gbSeq, optSeq, memberSeq, status, Timestamp.valueOf(now));
    }

    private GroupBuyStatus groupBuyStatus(long gbSeq) {
        return groupBuyRepository.findById(gbSeq).orElseThrow().getStatus();
    }

    private ParticipationStatus statusOf(long groupBuySeq, long memberSeq) {
        List<Participation> rows = participationRepository.findByGroupBuySeqAndMemberSeq(groupBuySeq, memberSeq);
        return rows.isEmpty() ? null : rows.get(0).getStatus();
    }

    @Test
    void 결제완료_인원이_최소인원_이상이면_확정된다() {
        // given: 최소 2명. 결제완료 2명
        long[] ids = createClosableGroupBuy(2);
        long gb = ids[0], opt = ids[1];
        insertParticipation(gb, opt, 1L, "PARTICIPATING");
        insertParticipation(gb, opt, 2L, "PARTICIPATING");

        // when
        groupBuyService.close(gb);

        // then: 공구·참여자 전원 CONFIRMED, 환불 없음(성사됐으니 결제 유지)
        assertThat(groupBuyStatus(gb)).as("공구 확정").isEqualTo(GroupBuyStatus.CONFIRMED);
        assertThat(statusOf(gb, 1L)).as("참여자 확정").isEqualTo(ParticipationStatus.CONFIRMED);
        assertThat(statusOf(gb, 2L)).isEqualTo(ParticipationStatus.CONFIRMED);
        verify(paymentPort, times(0)).refund(anyLong());

        // 알림 검증: 1·2번 회원에게 확정 알림(GROUP_BUY_CONFIRMED) 1건씩
        assertThat(notificationRepository.countByRecipientTypeAndRecipientSeqAndReadAtIsNull(
                NotificationRecipientType.MEMBER, 1L)).isEqualTo(1);
        assertThat(notificationRepository.countByRecipientTypeAndRecipientSeqAndReadAtIsNull(
                NotificationRecipientType.MEMBER, 2L)).isEqualTo(1);
    }

    @Test
    void 결제완료_인원이_최소인원_미달이면_무산되고_전원_환불된다() {
        // given: 최소 3명. 결제완료 2명 (미달)
        long[] ids = createClosableGroupBuy(3);
        long gb = ids[0], opt = ids[1];
        insertParticipation(gb, opt, 1L, "PARTICIPATING");
        insertParticipation(gb, opt, 2L, "PARTICIPATING");

        // when
        groupBuyService.close(gb);

        // then: 공구·참여자 전원 FAILED, 결제완료자 전원 1회씩 환불
        assertThat(groupBuyStatus(gb)).as("공구 무산").isEqualTo(GroupBuyStatus.FAILED);
        assertThat(statusOf(gb, 1L)).as("참여자 무산").isEqualTo(ParticipationStatus.FAILED);
        assertThat(statusOf(gb, 2L)).isEqualTo(ParticipationStatus.FAILED);
        verify(paymentPort, times(2)).refund(anyLong()); // 결제완료 2명 각각 환불 = 총 2회

        // 알림 검증: 1·2번 회원에게 무산(GROUP_BUY_FAILED) + 환불완료(REFUND_DONE) 2건씩
        List<Notification> noti1 = notificationRepository
                .findByRecipientTypeAndRecipientSeqOrderByCreatedAtDesc(NotificationRecipientType.MEMBER, 1L);
        assertThat(noti1).hasSize(2);
        assertThat(noti1.stream().map(Notification::getType))
                .containsExactlyInAnyOrder(NotificationType.GROUP_BUY_FAILED, NotificationType.REFUND_DONE);

        List<Notification> noti2 = notificationRepository
                .findByRecipientTypeAndRecipientSeqOrderByCreatedAtDesc(NotificationRecipientType.MEMBER, 2L);
        assertThat(noti2).hasSize(2);
        assertThat(noti2.stream().map(Notification::getType))
                .containsExactlyInAnyOrder(NotificationType.GROUP_BUY_FAILED, NotificationType.REFUND_DONE);
    }

    @Test
    void 마감시_결제대기자는_확정인원에서_제외되고_EXPIRED된다() {
        // given: 최소 2명. 결제완료 2명(확정) + 결제대기 1명
        long[] ids = createClosableGroupBuy(2);
        long gb = ids[0], opt = ids[1];
        insertParticipation(gb, opt, 1L, "PARTICIPATING");
        insertParticipation(gb, opt, 2L, "PARTICIPATING");
        insertParticipation(gb, opt, 3L, "PAYMENT_PENDING"); // 결제대기 — 확정 인원에서 제외돼야

        // when
        groupBuyService.close(gb);

        // then: 결제완료 2명으로 확정 판정(PAYMENT_PENDING 제외) / 결제대기자는 EXPIRED, 환불 없음
        assertThat(groupBuyStatus(gb)).as("결제완료 2명으로 확정").isEqualTo(GroupBuyStatus.CONFIRMED);
        assertThat(statusOf(gb, 3L)).as("결제대기자는 EXPIRED").isEqualTo(ParticipationStatus.EXPIRED);
        verify(paymentPort, times(0)).refund(anyLong()); // 확정이라 환불 없음(결제대기자 포함 아무도 환불 안 됨)

        // 알림 검증: 1·2번(확정)은 1건씩, 3번(만료)은 알림 없음
        assertThat(notificationRepository.countByRecipientTypeAndRecipientSeqAndReadAtIsNull(
                NotificationRecipientType.MEMBER, 1L)).isEqualTo(1);
        assertThat(notificationRepository.countByRecipientTypeAndRecipientSeqAndReadAtIsNull(
                NotificationRecipientType.MEMBER, 2L)).isEqualTo(1);
        assertThat(notificationRepository.countByRecipientTypeAndRecipientSeqAndReadAtIsNull(
                NotificationRecipientType.MEMBER, 3L)).isEqualTo(0);
    }
}
