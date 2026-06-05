-- ============================================================
-- 공동구매(공구) 관련 제약조건
-- 작성: 황윤재 / 대상 테이블: order_item, group_buy_options,
--       waiting_queue, participation
-- ============================================================

-- 1. order_item: 공구 주문엔 쿠폰/핫딜 할인 차단 (이중 방어)
--    participation_seq가 있으면(=공구) coupon/hotdeal 할인은 0이어야 함
ALTER TABLE order_item ADD CONSTRAINT CK_groupbuy_no_discount
CHECK (
  participation_seq IS NULL
  OR (coupon_discount = 0 AND hotdeal_discount = 0)
);

-- 2. group_buy_options: 한 공구에 같은 옵션 중복 등록 방지
ALTER TABLE group_buy_options ADD CONSTRAINT UQ_group_buy_options
UNIQUE (group_buy_seq, options_seq);

-- 3. waiting_queue: 한 사람이 같은 옵션 대기열에 중복 등록 방지
--    (대기 이탈을 '행 삭제'로 처리하는 전제. 재등록은 삭제 후 가능)
ALTER TABLE waiting_queue ADD CONSTRAINT UQ_waiting_queue_member
UNIQUE (group_buy_options_seq, member_seq);

-- 4. participation: 한 사람이 같은 공구에 활성 상태로 중복 참여 방지
--    CANCELLED/FAILED는 NULL 처리해 인덱스에서 제외 → 취소 후 재참여 허용
CREATE UNIQUE INDEX UQ_participation_active
ON participation (
  CASE WHEN status IN ('CANCELLED','FAILED') THEN NULL ELSE group_buy_seq END,
  CASE WHEN status IN ('CANCELLED','FAILED') THEN NULL ELSE member_seq END
);

-- 제약조건 적용 확인
SELECT table_name, constraint_name, constraint_type, search_condition
FROM   user_constraints
WHERE  table_name IN ('ORDER_ITEM','GROUP_BUY_OPTIONS','WAITING_QUEUE','PARTICIPATION')
ORDER  BY table_name, constraint_type;

SELECT c.table_name, c.constraint_name, c.constraint_type, cc.column_name, cc.position
FROM   user_constraints c
JOIN   user_cons_columns cc ON c.constraint_name = cc.constraint_name
WHERE  c.table_name IN ('GROUP_BUY_OPTIONS','WAITING_QUEUE','PARTICIPATION')
ORDER  BY c.table_name, c.constraint_name, cc.position;

-- 인덱스 자체 (UNIQUENESS = UNIQUE 인지 확인)
SELECT index_name, table_name, uniqueness, status
FROM   user_indexes
WHERE  table_name = 'PARTICIPATION';

-- 함수 기반 인덱스의 실제 표현식 (CASE WHEN ... 이 보여야 함)
SELECT index_name, column_position, column_expression
FROM   user_ind_expressions
WHERE  index_name = 'UQ_PARTICIPATION_ACTIVE'
ORDER  BY column_position;