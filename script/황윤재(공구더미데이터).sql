-- ==========================================================
-- 공동구매(group_buy) 더미데이터 — 작성: 황윤재
-- 목적: 공구 조회 API / 화면 검증용 (NPE·매진·임박마감·시작전 케이스 포함)
-- 전제: dummy_data.sql 이 먼저 실행되어 seller / category 가 존재해야 함
--      이미지 파일은 static/src/images/product/ 에 존재 (웹경로 /src/images/product/...)
-- 구성: 상품 5개 + 각 상품 옵션 + 대표이미지 + 공구 1건씩
-- DB: Oracle (PL/SQL 블록). 카테고리/판매처 seq는 이름으로 조회.
-- ==========================================================

DECLARE
    v_seller   NUMBER;   -- 판매처 (전 상품 공통)
    v_cat      NUMBER;   -- 카테고리 (상품별 조회)
    v_prod     NUMBER;   -- 상품 seq
    v_gb       NUMBER;   -- 공구 seq
    v_opt1     NUMBER;   -- 옵션 seq
    v_opt2     NUMBER;
    v_opt3     NUMBER;
BEGIN
    -- 판매처는 검증용이라 아무거나(판매처 1) 공통 사용
    SELECT seq INTO v_seller FROM seller WHERE name = '판매처 1' AND ROWNUM = 1;

    -- ===========================================================
    -- 1) 포칼 유토피아 헤드폰  | 헤드폰 | 500만→300만(40%) | ONGOING (평범)
    -- ===========================================================
    SELECT seq INTO v_cat FROM category WHERE category_name = '헤드폰' AND depth_level = 2 AND ROWNUM = 1;

    INSERT INTO product (seq, seller_seq, category_seq, product_name, price, content, approval_status)
    VALUES (product_seq.NEXTVAL, v_seller, v_cat, '포칼 유토피아 헤드폰', 5000000,
            '포칼 플래그십 오픈형 레퍼런스 헤드폰. 베릴륨 M자형 돔 드라이버 탑재.', 'APPROVED')
    RETURNING seq INTO v_prod;

    INSERT INTO options (seq, product_seq, color, stock, safety_stock, additional_price)
    VALUES (options_seq.NEXTVAL, v_prod, '블랙', 100, 10, 0) RETURNING seq INTO v_opt1;

    INSERT INTO product_image (seq, product_seq, image_url, public_id, thumbnail_yn)
    VALUES (product_image_seq.NEXTVAL, v_prod, '/src/images/product/New_Utopia_34.jpg', 'New_Utopia_34', 'Y');

    INSERT INTO group_buy (seq, product_seq, start_at, end_at, created_at, min_count, max_count, original_price, final_price, status)
    VALUES (group_buy_seq.NEXTVAL, v_prod,
            SYSTIMESTAMP - INTERVAL '1' DAY, SYSTIMESTAMP + INTERVAL '5' DAY, SYSTIMESTAMP,
            20, 50, 5000000, 3000000, 'ONGOING')
    RETURNING seq INTO v_gb;

    INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
    VALUES (group_buy_options_seq.NEXTVAL, v_gb, v_opt1, 50, 10);

    -- ===========================================================
    -- 2) 로지텍 지슈라 마우스 | 마우스 | 20만→14만(30%) | ONGOING (블랙 매진)
    -- ===========================================================
    SELECT seq INTO v_cat FROM category WHERE category_name = '마우스' AND depth_level = 2 AND ROWNUM = 1;

    INSERT INTO product (seq, seller_seq, category_seq, product_name, price, content, approval_status)
    VALUES (product_seq.NEXTVAL, v_seller, v_cat, '로지텍 지슈라 마우스', 200000,
            '로지텍 PRO X SUPERLIGHT 2 무선 게이밍 마우스. 60g 초경량.', 'APPROVED')
    RETURNING seq INTO v_prod;

    INSERT INTO options (seq, product_seq, color, stock, safety_stock, additional_price)
    VALUES (options_seq.NEXTVAL, v_prod, '블랙', 100, 10, 0) RETURNING seq INTO v_opt1;
    INSERT INTO options (seq, product_seq, color, stock, safety_stock, additional_price)
    VALUES (options_seq.NEXTVAL, v_prod, '화이트', 100, 10, 0) RETURNING seq INTO v_opt2;

    INSERT INTO product_image (seq, product_seq, image_url, public_id, thumbnail_yn)
    VALUES (product_image_seq.NEXTVAL, v_prod, '/src/images/product/gprox.jpg', 'gprox', 'Y');

    INSERT INTO group_buy (seq, product_seq, start_at, end_at, created_at, min_count, max_count, original_price, final_price, status)
    VALUES (group_buy_seq.NEXTVAL, v_prod,
            SYSTIMESTAMP - INTERVAL '2' DAY, SYSTIMESTAMP + INTERVAL '4' DAY, SYSTIMESTAMP,
            80, 200, 200000, 140000, 'ONGOING')
    RETURNING seq INTO v_gb;

    -- 블랙: occupied(100) = order_qty(100) → 매진(soldOut)
    INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
    VALUES (group_buy_options_seq.NEXTVAL, v_gb, v_opt1, 100, 100);
    -- 화이트: 여유
    INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
    VALUES (group_buy_options_seq.NEXTVAL, v_gb, v_opt2, 100, 30);

    -- ===========================================================
    -- 3) 에이수스 제피러스 노트북 | 노트북 | 200만→160만(20%) | ONGOING (임박마감 2h)
    -- ===========================================================
    SELECT seq INTO v_cat FROM category WHERE category_name = '노트북' AND depth_level = 2 AND ROWNUM = 1;

    INSERT INTO product (seq, seller_seq, category_seq, product_name, price, content, approval_status)
    VALUES (product_seq.NEXTVAL, v_seller, v_cat, '에이수스 제피러스 노트북', 2000000,
            'ASUS ROG Zephyrus G16 게이밍 노트북. OLED 디스플레이.', 'APPROVED')
    RETURNING seq INTO v_prod;

    INSERT INTO options (seq, product_seq, color, stock, safety_stock, additional_price)
    VALUES (options_seq.NEXTVAL, v_prod, '그레이', 100, 10, 0) RETURNING seq INTO v_opt1;

    INSERT INTO product_image (seq, product_seq, image_url, public_id, thumbnail_yn)
    VALUES (product_image_seq.NEXTVAL, v_prod, '/src/images/product/asusnotebook.png', 'asusnotebook', 'Y');

    INSERT INTO group_buy (seq, product_seq, start_at, end_at, created_at, min_count, max_count, original_price, final_price, status)
    VALUES (group_buy_seq.NEXTVAL, v_prod,
            SYSTIMESTAMP - INTERVAL '3' DAY, SYSTIMESTAMP + INTERVAL '2' HOUR, SYSTIMESTAMP,
            15, 30, 2000000, 1600000, 'ONGOING')
    RETURNING seq INTO v_gb;

    INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
    VALUES (group_buy_options_seq.NEXTVAL, v_gb, v_opt1, 30, 12);

    -- ===========================================================
    -- 4) 뉴발란스 992 신발 | 운동화 | 30만→26만(13%) | SCHEDULED (시작전)
    -- ===========================================================
    SELECT seq INTO v_cat FROM category WHERE category_name = '운동화' AND depth_level = 2 AND ROWNUM = 1;

    INSERT INTO product (seq, seller_seq, category_seq, product_name, price, content, approval_status)
    VALUES (product_seq.NEXTVAL, v_seller, v_cat, '뉴발란스 992 신발', 300000,
            '뉴발란스 992 메이드 인 USA. 그레이 클래식 컬러.', 'APPROVED')
    RETURNING seq INTO v_prod;

    INSERT INTO options (seq, product_seq, options_size, stock, safety_stock, additional_price)
    VALUES (options_seq.NEXTVAL, v_prod, '260', 100, 10, 0) RETURNING seq INTO v_opt1;
    INSERT INTO options (seq, product_seq, options_size, stock, safety_stock, additional_price)
    VALUES (options_seq.NEXTVAL, v_prod, '270', 100, 10, 0) RETURNING seq INTO v_opt2;
    INSERT INTO options (seq, product_seq, options_size, stock, safety_stock, additional_price)
    VALUES (options_seq.NEXTVAL, v_prod, '280', 100, 10, 0) RETURNING seq INTO v_opt3;

    INSERT INTO product_image (seq, product_seq, image_url, public_id, thumbnail_yn)
    VALUES (product_image_seq.NEXTVAL, v_prod, '/src/images/product/newbalance992.jpg', 'newbalance992', 'Y');

    -- 시작전: start_at 이 미래
    INSERT INTO group_buy (seq, product_seq, start_at, end_at, created_at, min_count, max_count, original_price, final_price, status)
    VALUES (group_buy_seq.NEXTVAL, v_prod,
            SYSTIMESTAMP + INTERVAL '2' DAY, SYSTIMESTAMP + INTERVAL '9' DAY, SYSTIMESTAMP,
            50, 120, 300000, 260000, 'SCHEDULED')
    RETURNING seq INTO v_gb;

    -- 시작 전이라 점유 0
    INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
    VALUES (group_buy_options_seq.NEXTVAL, v_gb, v_opt1, 40, 0);
    INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
    VALUES (group_buy_options_seq.NEXTVAL, v_gb, v_opt2, 40, 0);
    INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
    VALUES (group_buy_options_seq.NEXTVAL, v_gb, v_opt3, 40, 0);

    -- ===========================================================
    -- 5) 젠하이저 IE900 이어폰 | 이어폰 | 100만→60만(40%) | ONGOING (거의 참, 잔여 5)
    -- ===========================================================
    SELECT seq INTO v_cat FROM category WHERE category_name = '이어폰' AND depth_level = 2 AND ROWNUM = 1;

    INSERT INTO product (seq, seller_seq, category_seq, product_name, price, content, approval_status)
    VALUES (product_seq.NEXTVAL, v_seller, v_cat, '젠하이저 IE900 이어폰', 1000000,
            '젠하이저 플래그십 유선 이어폰 IE 900. X3R 트랜스듀서.', 'APPROVED')
    RETURNING seq INTO v_prod;

    INSERT INTO options (seq, product_seq, color, stock, safety_stock, additional_price)
    VALUES (options_seq.NEXTVAL, v_prod, '실버', 100, 10, 0) RETURNING seq INTO v_opt1;

    INSERT INTO product_image (seq, product_seq, image_url, public_id, thumbnail_yn)
    VALUES (product_image_seq.NEXTVAL, v_prod, '/src/images/product/ie900.jpg', 'ie900', 'Y');

    INSERT INTO group_buy (seq, product_seq, start_at, end_at, created_at, min_count, max_count, original_price, final_price, status)
    VALUES (group_buy_seq.NEXTVAL, v_prod,
            SYSTIMESTAMP - INTERVAL '1' DAY, SYSTIMESTAMP + INTERVAL '6' DAY, SYSTIMESTAMP,
            30, 60, 1000000, 600000, 'ONGOING')
    RETURNING seq INTO v_gb;

    -- occupied 55 / order_qty 60 → 잔여 5 (거의 참)
    INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
    VALUES (group_buy_options_seq.NEXTVAL, v_gb, v_opt1, 60, 55);

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('공구 더미데이터 5건 등록 완료.');

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('공구 더미 생성 중 오류: ' || SQLERRM);
        RAISE;
END;
/

DECLARE
  v_prod   NUMBER;
  v_gb     NUMBER;
  v_maxcnt NUMBER := 0;
BEGIN
  -- 뉴발란스 상품 재사용 (옵션 260/270/280 그대로)
  SELECT seq INTO v_prod FROM product WHERE product_name = '뉴발란스 992 신발' AND ROWNUM = 1;

  -- 새 진행중 공구 (마감 7일 뒤, 최소인원 1이라 테스트 쉬움)
  INSERT INTO group_buy (seq, product_seq, start_at, end_at, created_at,
                         min_count, max_count, original_price, final_price, status)
  VALUES (group_buy_seq.NEXTVAL, v_prod,
          SYSTIMESTAMP - INTERVAL '1' HOUR, SYSTIMESTAMP + INTERVAL '7' DAY, SYSTIMESTAMP,
          1, 0, 300000, 260000, 'ONGOING')
  RETURNING seq INTO v_gb;

  -- 이 상품의 모든 옵션을 공구 옵션으로 (정원 40, 점유 0 → 자리 넉넉)
  FOR o IN (SELECT seq FROM options WHERE product_seq = v_prod) LOOP
    INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
    VALUES (group_buy_options_seq.NEXTVAL, v_gb, o.seq, 40, 0);
    v_maxcnt := v_maxcnt + 40;
  END LOOP;

  UPDATE group_buy SET max_count = v_maxcnt WHERE seq = v_gb;

  COMMIT;
  DBMS_OUTPUT.PUT_LINE('새 공구 seq = ' || v_gb);
END;
/

UPDATE group_buy
SET status   = 'ONGOING',
    start_at = TIMESTAMP '2026-06-12 14:00:00'
WHERE seq = 9;
COMMIT;

-- 공구 테스트용 더미 20개 더 만들기(상품 20개 + 그 상품에 대한 공구 20개)

DECLARE
    v_seller NUMBER;
    v_cat    NUMBER;
    v_prod   NUMBER;
    v_gb     NUMBER;
    v_opt    NUMBER;
BEGIN
    SELECT seq INTO v_seller FROM seller   WHERE name = '판매처 1' AND ROWNUM = 1;
    SELECT seq INTO v_cat    FROM category WHERE category_name = '운동화' AND depth_level = 2 AND ROWNUM = 1;

    FOR i IN 1..20 LOOP
        -- 상품 (이름에 번호 붙여 화면에서 구분)
        INSERT INTO product (seq, seller_seq, category_seq, product_name, price, content, approval_status)
        VALUES (product_seq.NEXTVAL, v_seller, v_cat, '뉴발란스 992 신발 (' || i || ')', 300000,
                '테스트용 진행중 공구 더미', 'APPROVED')
        RETURNING seq INTO v_prod;

        -- 대표 이미지 (기존 뉴발란스 이미지 재사용)
        INSERT INTO product_image (seq, product_seq, image_url, public_id, thumbnail_yn)
        VALUES (product_image_seq.NEXTVAL, v_prod, '/src/images/product/newbalance992.jpg', 'newbalance992', 'Y');

        -- 진행중 공구 (1시간 전 시작 ~ 7일 뒤 마감, min_count=1 이라 테스트 쉬움)
        INSERT INTO group_buy (seq, product_seq, start_at, end_at, created_at,
                               min_count, max_count, original_price, final_price, status)
        VALUES (group_buy_seq.NEXTVAL, v_prod,
                SYSTIMESTAMP - INTERVAL '1' HOUR, SYSTIMESTAMP + INTERVAL '7' DAY, SYSTIMESTAMP,
                1, 120, 300000, 260000, 'ONGOING')
        RETURNING seq INTO v_gb;

        -- 옵션 260/270/280 (각 정원 40, 점유 0 → 자리 넉넉, max_count=120과 일치)
        INSERT INTO options (seq, product_seq, options_size, stock, safety_stock, additional_price)
        VALUES (options_seq.NEXTVAL, v_prod, '260', 100, 10, 0) RETURNING seq INTO v_opt;
        INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
        VALUES (group_buy_options_seq.NEXTVAL, v_gb, v_opt, 40, 0);

        INSERT INTO options (seq, product_seq, options_size, stock, safety_stock, additional_price)
        VALUES (options_seq.NEXTVAL, v_prod, '270', 100, 10, 0) RETURNING seq INTO v_opt;
        INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
        VALUES (group_buy_options_seq.NEXTVAL, v_gb, v_opt, 40, 0);

        INSERT INTO options (seq, product_seq, options_size, stock, safety_stock, additional_price)
        VALUES (options_seq.NEXTVAL, v_prod, '280', 100, 10, 0) RETURNING seq INTO v_opt;
        INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
        VALUES (group_buy_options_seq.NEXTVAL, v_gb, v_opt, 40, 0);
    END LOOP;

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('진행중 공구 더미 20건 등록 완료.');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('오류: ' || SQLERRM);
        RAISE;
END;
/

-- 관련 시퀀스를 각 테이블의 max(seq)+1로 맞추는 보정 SQL

DECLARE
    v_max NUMBER;
BEGIN
    SELECT NVL(MAX(seq),0)+1 INTO v_max FROM product;
    EXECUTE IMMEDIATE 'ALTER SEQUENCE product_seq RESTART START WITH ' || v_max;

    SELECT NVL(MAX(seq),0)+1 INTO v_max FROM options;
    EXECUTE IMMEDIATE 'ALTER SEQUENCE options_seq RESTART START WITH ' || v_max;

    SELECT NVL(MAX(seq),0)+1 INTO v_max FROM product_image;
    EXECUTE IMMEDIATE 'ALTER SEQUENCE product_image_seq RESTART START WITH ' || v_max;

    SELECT NVL(MAX(seq),0)+1 INTO v_max FROM group_buy;
    EXECUTE IMMEDIATE 'ALTER SEQUENCE group_buy_seq RESTART START WITH ' || v_max;

    SELECT NVL(MAX(seq),0)+1 INTO v_max FROM group_buy_options;
    EXECUTE IMMEDIATE 'ALTER SEQUENCE group_buy_options_seq RESTART START WITH ' || v_max;

    DBMS_OUTPUT.PUT_LINE('시퀀스 5개 보정 완료.');
END;
/