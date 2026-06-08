select * from options;

select * from PRODUCT where seq = 1080;

select * from COUPON;

select * from DELIVERY_ADDRESS;

select * from MEMBER;

select * from CART;

select * from orders;

select * from ORDER_ITEM;


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

COMMIT;