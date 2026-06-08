-- 공구 참여 동시성 테스트 전용 최소 스키마 (Testcontainers Oracle XE init script).
-- 실제 ddl.sql 정의와 동일하되, 락/참여 흐름과 무관한 FK는 생략한다.
-- participate()가 group_buy 상태 확인 → 옵션 점유 → participation INSERT 까지 하므로
-- group_buy / group_buy_options / participation 세 테이블이 필요하다.

CREATE TABLE group_buy (
    seq number NOT NULL PRIMARY KEY,
    product_seq number NOT NULL,
    start_at timestamp NOT NULL,
    end_at timestamp NOT NULL,
    created_at timestamp NOT NULL,
    finished_at timestamp,
    min_count number NOT NULL,
    max_count number NOT NULL,
    original_price number NOT NULL,
    final_price number NOT NULL,
    status varchar2(20) NOT NULL
);

CREATE TABLE group_buy_options (
    seq number NOT NULL PRIMARY KEY,
    group_buy_seq number NOT NULL,
    options_seq number NOT NULL,
    order_qty number NOT NULL,
    occupied_count number DEFAULT 0 NOT NULL
);

CREATE TABLE participation (
    seq number NOT NULL PRIMARY KEY,
    group_buy_seq number NOT NULL,
    group_buy_options_seq number NOT NULL,
    member_seq number NOT NULL,
    status varchar2(20) NOT NULL,
    payment_deadline timestamp,
    promoted_at timestamp,
    created_at timestamp NOT NULL
);

-- participation 은 save()로 INSERT 되므로 시퀀스가 필요하다 (allocationSize=1 → NOCACHE).
CREATE SEQUENCE participation_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- 진행 중(ONGOING) 공구 1건. end_at 은 타임존 영향 없이 항상 미래가 되도록 먼 미래 고정값.
INSERT INTO group_buy (seq, product_seq, start_at, end_at, created_at, min_count, max_count, original_price, final_price, status)
VALUES (1, 1, TIMESTAMP '2000-01-01 00:00:00', TIMESTAMP '2999-12-31 23:59:59', TIMESTAMP '2000-01-01 00:00:00', 1, 10, 10000, 8000, 'ONGOING');

-- 테스트 대상 옵션 행: 발주 가능 10자리, 점유 0에서 시작.
INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
VALUES (1, 1, 1, 10, 0);
