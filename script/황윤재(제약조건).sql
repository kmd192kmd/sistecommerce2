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
--    CANCELLED/FAILED/EXPIRED는 NULL 처리해 인덱스에서 제외 → 취소·무산·만료 후 재참여 허용
--    (EXPIRED = 승격 후 결제기한 미결제로 자격 소멸. 결제 놓친 사람도 다시 참여 가능해야 하므로 제외)
CREATE UNIQUE INDEX UQ_participation_active
ON participation (
  CASE WHEN status IN ('CANCELLED','FAILED','EXPIRED') THEN NULL ELSE group_buy_seq END,
  CASE WHEN status IN ('CANCELLED','FAILED','EXPIRED') THEN NULL ELSE member_seq END
);

-- UQ_participation_active 재생성: EXPIRED도 인덱스에서 제외 (만료자 재참여 허용)
DROP INDEX UQ_participation_active;

CREATE UNIQUE INDEX UQ_participation_active
ON participation (
  CASE WHEN status IN ('CANCELLED','FAILED','EXPIRED') THEN NULL ELSE group_buy_seq END,
  CASE WHEN status IN ('CANCELLED','FAILED','EXPIRED') THEN NULL ELSE member_seq END
);

-- ① 진행중 공구 + 옵션 (어디에 참여자를 넣을지)
SELECT gb.seq AS gb_seq, gb.status, gbo.seq AS gbo_seq,
       gbo.order_qty, gbo.occupied_count, gb.min_count
FROM group_buy gb
JOIN group_buy_options gbo ON gbo.group_buy_seq = gb.seq
WHERE gb.status = 'ONGOING'
ORDER BY gb.seq, gbo.seq;

-- ② 참여시킬 회원 seq (앞쪽 20명)
SELECT seq FROM member WHERE ROWNUM <= 20 ORDER BY seq;

-- 공구 4번에 회원 15명을 옵션 3개(5,6,7)에 균등 분배해 정규참여(PARTICIPATING) 생성
INSERT INTO participation (seq, group_buy_seq, group_buy_options_seq, member_seq, status, created_at)
SELECT participation_seq.NEXTVAL,
       4,
       CASE MOD(rn, 3) WHEN 0 THEN 5 WHEN 1 THEN 6 ELSE 7 END,
       seq,
       'PARTICIPATING',
       SYSTIMESTAMP
FROM (
    SELECT seq, ROWNUM rn
    FROM (SELECT seq FROM member ORDER BY seq)
    WHERE ROWNUM <= 15
);

-- occupied_count를 실제 활성 참여 수와 동기화 (NFR-003: 점유 = PARTICIPATING + PAYMENT_PENDING 수)
UPDATE group_buy_options gbo
SET occupied_count = (
    SELECT COUNT(*) FROM participation p
    WHERE p.group_buy_options_seq = gbo.seq
      AND p.status IN ('PARTICIPATING','PAYMENT_PENDING')
)
WHERE gbo.group_buy_seq = 4;

-- 공구 4번 옵션 중 일부에 추가금 부여 (확인용). gbo 6,7 → 그 옵션들에 +10000, +20000
UPDATE options SET additional_price = 10000 WHERE seq = (SELECT options_seq FROM group_buy_options WHERE seq = 6);
UPDATE options SET additional_price = 20000 WHERE seq = (SELECT options_seq FROM group_buy_options WHERE seq = 7);
COMMIT;