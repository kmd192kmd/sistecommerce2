-- 비관적 락 동시성 테스트 전용 최소 스키마 (Testcontainers Oracle XE init script).
-- 실제 ddl.sql의 group_buy_options 정의와 동일하되, 락 동작과 무관한 FK는 생략한다.
-- participate()는 이 행을 FOR UPDATE로 잠그고 occupied_count만 갱신하므로 부모 테이블이 필요 없다.
CREATE TABLE group_buy_options (
    seq number NOT NULL PRIMARY KEY,
    group_buy_seq number NOT NULL,
    options_seq number NOT NULL,
    order_qty number NOT NULL,
    occupied_count number DEFAULT 0 NOT NULL
);

-- 테스트 대상 행: 발주 가능 10자리, 점유 0에서 시작.
INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
VALUES (1, 1, 1, 10, 0);
