ALTER TABLE orders DROP COLUMN order_item_seq;


ALTER TABLE order_item ADD order_seq NUMBER;


ALTER TABLE order_item
ADD CONSTRAINT fk_order_item_order
FOREIGN KEY (order_seq) REFERENCES orders(seq);


ALTER TABLE orders DROP COLUMN order_item_seq;



SELECT constraint_name
FROM user_constraints
WHERE table_name = 'ORDERS'
  AND constraint_type = 'R';

SELECT
    uc.constraint_name,
    ucc.column_name,
    uc.r_constraint_name
FROM user_constraints uc
JOIN user_cons_columns ucc
    ON uc.constraint_name = ucc.constraint_name
WHERE uc.table_name = 'ORDERS'
  AND uc.constraint_type = 'R'
ORDER BY uc.constraint_name, ucc.position;



select * from ORDERS;

select * from ORDER_ITEM;

ALTER TABLE payment MODIFY receipt_url VARCHAR2(1000);
ALTER TABLE payment MODIFY external_payment_id VARCHAR2(200);
ALTER TABLE payment MODIFY pg_tid VARCHAR2(200);
ALTER TABLE payment MODIFY receipt_url VARCHAR2(1000);
ALTER TABLE payment MODIFY fail_reason VARCHAR2(1000);

ALTER TABLE refund MODIFY return_request NULL;

ALTER TABLE refund MODIFY returns_seq NULL;


commit;