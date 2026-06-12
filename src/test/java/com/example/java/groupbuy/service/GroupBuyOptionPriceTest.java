package com.example.java.groupbuy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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

import com.example.java.groupbuy.dto.GroupBuyOptionView;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.orders.dto.OrderCreateResultDto;
import com.example.java.orders.service.OrdersCommandService;
import com.example.java.product.entity.Options;

/**
 * 옵션별 가격(공구 기준 할인가 + options.additional_price) 검증.
 *
 * 팀 합의: 제일 싼 옵션을 기준가로 두고 additional_price(0 이상)로 옵션별 가격차를 표현한다.
 * 공구도 이를 따라 결제/환불/응답 가격을 (final_price + additional_price)로 계산한다.
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.hikari.maximum-pool-size=10"
})
class GroupBuyOptionPriceTest {

    @Container
    @ServiceConnection
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            .withInitScript("groupbuy-lock-schema.sql");

    @Autowired
    GroupBuyService groupBuyService;

    @Autowired
    JdbcTemplate jdbc;

    // participate가 호출하는 공구 주문 생성(orders 도메인)은 테스트 스키마에 테이블이 없으므로 모킹.
    // 이 테스트의 관심사는 '넘기는 금액(finalPrice)'이라, 그 인자만 검증한다.
    @MockitoBean
    OrdersCommandService ordersCommandService;

    private static final AtomicLong SEQ = new AtomicLong(900);

    /** 공구 기준가 8000 + 추가금이 지정된 옵션 1건을 만든다. @return [groupBuySeq, groupBuyOptionSeq] */
    private long[] createWithAdditional(int orderQty, int additionalPrice) {
        long gbSeq = SEQ.getAndIncrement();
        long gboSeq = SEQ.getAndIncrement();
        long optionsSeq = SEQ.getAndIncrement();
        LocalDateTime past = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime future = LocalDateTime.now().plusDays(10);
        jdbc.update("""
                INSERT INTO group_buy
                (seq, product_seq, start_at, end_at, created_at, min_count, max_count, original_price, final_price, status)
                VALUES (?, 1, ?, ?, ?, 1, ?, 10000, 8000, 'ONGOING')
                """,
                gbSeq, Timestamp.valueOf(past), Timestamp.valueOf(future), Timestamp.valueOf(past), orderQty);
        jdbc.update("INSERT INTO options (seq, stock, safety_stock, additional_price) VALUES (?, 0, 0, ?)",
                optionsSeq, additionalPrice);
        jdbc.update("""
                INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
                VALUES (?, ?, ?, ?, 0)
                """,
                gboSeq, gbSeq, optionsSeq, orderQty);
        return new long[]{gbSeq, gboSeq};
    }

    @Test
    void 추가금_있는_옵션은_공구기준가에_추가금이_더해져_결제된다() {
        // given: 기준가 8000 + 추가금 3000 옵션
        long[] ids = createWithAdditional(10, 3000);

        when(ordersCommandService.createGroupBuyOrder(anyLong(), anyLong(), anyLong(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new OrderCreateResultDto(1L, "GB-TEST", 11000, "테스트상품"));

        // when: 참여(자리 예약 + 결제 대기 주문 생성)
        groupBuyService.participate(ids[0], ids[1], 1L);

        // then: 결제 대기 주문 금액 = 8000 + 3000 = 11000
        verify(ordersCommandService).createGroupBuyOrder(eq(1L), anyLong(), anyLong(), anyInt(), eq(11000), anyInt());
    }

    @Test
    void 추가금이_0인_옵션은_공구기준가_그대로_결제된다() {
        // given: 기준가 8000 + 추가금 0 (제일 싼 옵션)
        long[] ids = createWithAdditional(10, 0);

        when(ordersCommandService.createGroupBuyOrder(anyLong(), anyLong(), anyLong(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new OrderCreateResultDto(1L, "GB-TEST", 8000, "테스트상품"));

        groupBuyService.participate(ids[0], ids[1], 2L);

        // then: 결제 대기 주문 금액 = 8000 + 0 = 8000
        verify(ordersCommandService).createGroupBuyOrder(eq(2L), anyLong(), anyLong(), anyInt(), eq(8000), anyInt());
    }

    @Test
    void 응답DTO의_옵션가격은_기준가에_추가금을_더한값이다() {
        // GroupBuyOptionView.from 의 가격 계산만 떼어 순수 검증 (DB 조회 없음).
        // getDetail 전체는 product/product_image까지 로딩하므로, 가격 로직만 분리해 본다.
        Options opt = Options.builder().seq(1L).additionalPrice(5000).build();
        GroupBuyOptions gbo = GroupBuyOptions.builder()
                .options(opt).orderQty(10).occupiedCount(0).build();

        GroupBuyOptionView view = GroupBuyOptionView.from(gbo, 8000); // 공구 기준가 8000

        assertThat(view.getFinalPrice()).as("옵션 가격 = 기준가 8000 + 추가금 5000").isEqualTo(13000);
        assertThat(view.isSoldOut()).as("점유 0 < 정원 10 → 매진 아님").isFalse();
    }
}
