select * from options where product_seq = 1680;



select * from PRODUCT order by seq desc;

select * from COUPON;

select * from member_coupon where member_seq = 9;

select * from DELIVERY_ADDRESS;

select * from MEMBER where seq = 9;

select * from CART_log order by seq desc;

select * from orders order by seq desc;

select * from orders;

select * from ORDER_ITEM order by seq desc;

select * from login_log;

select * from seller;

select * from PAYMENT;

select * from refund;


1656    1678    ->     3342         3355

1677            ->      3354

1671     1681   ->      3312        3361

DELETE FROM ORDERS
WHERE seq <= 5;

DELETE FROM ORDER_ITEM
WHERE seq <= 10;

INSERT INTO coupon (
    seq,
    name,
    discount_type,
    start_date,
    valid_days,
    status,
    discount_price,
    discount_rate
) VALUES (
    80,
    '결제 테스트 10% 할인 쿠폰',
    0,
    SYSDATE,
    30,
    1,
    NULL,
    10
);

INSERT INTO coupon (
    seq,
    name,
    discount_type,
    start_date,
    valid_days,
    status,
    discount_price,
    discount_rate
) VALUES (
    81,
    '결제 테스트 3000원 할인 쿠폰',
    1,
    SYSDATE,
    30,
    1,
    3000,
    NULL
);

/* 레코드 관계 확인 */
SELECT
    c.table_name      AS child_table,
    cc.column_name    AS child_column,
    c.constraint_name AS fk_name
FROM user_constraints c
JOIN user_cons_columns cc
    ON c.constraint_name = cc.constraint_name
WHERE c.constraint_type = 'R'
  AND c.r_constraint_name IN (
      SELECT constraint_name
      FROM user_constraints
      WHERE table_name = 'ORDER_ITEM'
        AND constraint_type IN ('P', 'U')
  )
ORDER BY c.table_name, cc.position;

COMMIT;