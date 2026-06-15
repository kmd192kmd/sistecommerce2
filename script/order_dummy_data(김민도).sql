-- Creates one sample order that contains:
-- - 2 order_item rows from delivery company A
-- - 2 order_item rows from delivery company B
--
-- This script does not change application order code. It only inserts data that
-- follows the current runtime model:
--   orders.seq <- order_item.order_seq
--   order_item -> options -> product -> seller -> delivery_company
--
-- Optional:
--   Change v_target_username to the account you use when opening /mypage/orders.
--   If it is NULL, the first member row is used.

SET SERVEROUTPUT ON;

DECLARE
    v_target_username member.username%TYPE := NULL;

    v_member_seq member.seq%TYPE;
    v_order_seq orders.seq%TYPE;
    v_order_uid orders.order_uid%TYPE;
    v_product_total NUMBER := 0;

    TYPE t_company_seq_list IS TABLE OF delivery_company.seq%TYPE INDEX BY PLS_INTEGER;
    TYPE t_company_name_list IS TABLE OF delivery_company.name%TYPE INDEX BY PLS_INTEGER;

    v_company_seqs t_company_seq_list;
    v_company_names t_company_name_list;
BEGIN
    IF v_target_username IS NOT NULL THEN
        SELECT seq
          INTO v_member_seq
          FROM member
         WHERE username = v_target_username;
    ELSE
        SELECT seq
          INTO v_member_seq
          FROM (
                SELECT seq
                  FROM member
                 ORDER BY seq
               )
         WHERE ROWNUM = 1;
    END IF;

    SELECT orders_seq.NEXTVAL
      INTO v_order_seq
      FROM dual;

    v_order_uid := 'SAMPLE-GROUPED-' || v_order_seq || '-' || TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS');

    INSERT INTO orders (
        seq,
        member_seq,
        member_coupon_seq,
        order_uid,
        product_total_price,
        coupon_discount,
        hotdeal_discount,
        field,
        final_price,
        total_refund_price,
        remain_price,
        order_status,
        payment_status,
        order_date,
        regdate,
        zipcode,
        address,
        address_detail,
        curr_latitude,
        curr_longitude
    ) VALUES (
        v_order_seq,
        v_member_seq,
        NULL,
        v_order_uid,
        0,
        0,
        0,
        '택배사별 묶음 테스트 주문',
        0,
        0,
        0,
        5,
        2,
        SYSDATE,
        SYSDATE,
        '06193',
        '서울특별시 강남구 테스트로 12',
        '택배사별 묶음 테스트',
        37.5665000,
        126.9780000
    );

    FOR item_row IN (
        WITH company_candidates AS (
            SELECT dc.seq
              FROM delivery_company dc
              JOIN seller s ON s.delivery_company_seq = dc.seq
              JOIN product p ON p.seller_seq = s.seq
              JOIN options opt ON opt.product_seq = p.seq
             GROUP BY dc.seq
            HAVING COUNT(*) >= 2
        ),
        selected_companies AS (
            SELECT seq,
                   ROW_NUMBER() OVER (ORDER BY seq) AS company_no
              FROM company_candidates
             WHERE ROWNUM <= 2
        ),
        selected_items AS (
            SELECT sc.company_no,
                   dc.seq AS delivery_company_seq,
                   dc.name AS delivery_company_name,
                   opt.seq AS options_seq,
                   p.product_name,
                   p.price + NVL(opt.additional_price, 0) AS unit_price,
                   ROW_NUMBER() OVER (
                       PARTITION BY dc.seq
                       ORDER BY p.seq, opt.seq
                   ) AS item_no
              FROM selected_companies sc
              JOIN delivery_company dc ON dc.seq = sc.seq
              JOIN seller s ON s.delivery_company_seq = dc.seq
              JOIN product p ON p.seller_seq = s.seq
              JOIN options opt ON opt.product_seq = p.seq
        )
        SELECT *
          FROM selected_items
         WHERE item_no <= 2
         ORDER BY company_no, item_no
    ) LOOP
        INSERT INTO order_item (
            seq,
            order_seq,
            participation_seq,
            options_seq,
            product_name,
            quantity,
            original_price,
            hotdeal_discount,
            coupon_discount,
            participation_discount,
            final_price,
            sub_total_price,
            refund_quantity,
            refund_price,
            item_status,
            return_quantity
        ) VALUES (
            order_item_seq.NEXTVAL,
            v_order_seq,
            NULL,
            item_row.options_seq,
            item_row.product_name,
            1,
            item_row.unit_price,
            0,
            0,
            0,
            item_row.unit_price,
            item_row.unit_price,
            0,
            0,
            CASE WHEN item_row.company_no = 1 THEN 3 ELSE 2 END,
            NULL
        );

        v_product_total := v_product_total + item_row.unit_price;
        v_company_seqs(item_row.company_no) := item_row.delivery_company_seq;
        v_company_names(item_row.company_no) := item_row.delivery_company_name;
    END LOOP;

    IF v_company_seqs.COUNT < 2 THEN
        RAISE_APPLICATION_ERROR(
            -20001,
            'Need at least two delivery companies with at least two options each.'
        );
    END IF;

    UPDATE orders
       SET product_total_price = v_product_total,
           final_price = v_product_total,
           remain_price = v_product_total
     WHERE seq = v_order_seq;

    INSERT INTO delivery (
        seq,
        tracking_number,
        recipient_name,
        recipient_phone,
        status,
        request_memo,
        dispatch_at,
        estimated_date,
        completed_at,
        distance_surcharge,
        total_delivery_fee,
        delivery_company_seq,
        orders_seq,
        purchase_order_seq
    ) VALUES (
        delivery_seq.NEXTVAL,
        'SAMPLE-' || v_order_seq || '-A',
        '테스트회원',
        '010-0000-0000',
        'DELIVERED',
        '택배사 A 묶음 테스트',
        SYSDATE - 2,
        SYSDATE - 1,
        SYSDATE,
        0,
        3000,
        v_company_seqs(1),
        v_order_seq,
        NULL
    );

    INSERT INTO delivery (
        seq,
        tracking_number,
        recipient_name,
        recipient_phone,
        status,
        request_memo,
        dispatch_at,
        estimated_date,
        completed_at,
        distance_surcharge,
        total_delivery_fee,
        delivery_company_seq,
        orders_seq,
        purchase_order_seq
    ) VALUES (
        delivery_seq.NEXTVAL,
        'SAMPLE-' || v_order_seq || '-B',
        '테스트회원',
        '010-0000-0000',
        'SHIPPING',
        '택배사 B 묶음 테스트',
        SYSDATE - 1,
        SYSDATE + 1,
        NULL,
        0,
        3000,
        v_company_seqs(2),
        v_order_seq,
        NULL
    );

    COMMIT;

    DBMS_OUTPUT.PUT_LINE('Created sample grouped order.');
    DBMS_OUTPUT.PUT_LINE('orders.seq = ' || v_order_seq);
    DBMS_OUTPUT.PUT_LINE('orders.order_uid = ' || v_order_uid);
    DBMS_OUTPUT.PUT_LINE('member.seq = ' || v_member_seq);
    DBMS_OUTPUT.PUT_LINE('company A = ' || v_company_names(1));
    DBMS_OUTPUT.PUT_LINE('company B = ' || v_company_names(2));
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('Failed to create sample grouped order: ' || SQLERRM);
        RAISE;
END;
/
