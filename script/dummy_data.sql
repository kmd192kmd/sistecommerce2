-- ==========================================================
-- 쇼핑몰 프로젝트 대량 더미데이터 생성 스크립트 (1000건 이상)
-- 작성 목적: 테스트 및 검색/페이징 기능 검증을 위한 대량 데이터 구축
-- 구성 내역:
--   - 택배사: 4개
--   - 판매처: 10개 (각 택배사 골고루 분배)
--   - 카테고리: 대분류(3개) -> 중분류(6개) -> 소분류(18개, depth_level=2)
--   - 상품: 1050개 (모든 상품은 소분류 카테고리를 참조하도록 매핑)
--   - 상품 옵션: 상품당 2개씩 총 2100개
--   - 상품 이미지: 상품당 1개씩 총 1050개
-- 데이터베이스: Oracle Database (PL/SQL 블록 사용)
-- ==========================================================

DECLARE
    -- 생성된 ID들을 임시로 저장할 컬렉션 선언
    TYPE t_num_list IS TABLE OF NUMBER INDEX BY BINARY_INTEGER;
    v_delivery_ids t_num_list;
    v_seller_ids t_num_list;
    v_subcat_ids t_num_list;
    
    v_seq NUMBER;
    v_parent_seq NUMBER;
    v_sub_parent_seq NUMBER;
    v_idx NUMBER;
    
    v_price NUMBER;
    v_seller_seq NUMBER;
    v_cat_seq NUMBER;
    v_prod_seq NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('대량 더미 데이터 생성을 시작합니다...');

    -- 1. 택배사 (delivery_company) 4개 생성
    FOR i IN 1..4 LOOP
        INSERT INTO delivery_company (seq, name, customer_service_phone, base_delivery_fee, monthly_fee)
        VALUES (delivery_company_seq.NEXTVAL, '택배사 ' || i, '1588-000' || i, 2500 + i*100, 300000 + i*50000)
        RETURNING seq INTO v_seq;
        v_delivery_ids(i) := v_seq;
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('택배사 4개 등록 완료.');

    -- 2. 판매처 (seller) 10개 생성
    FOR i IN 1..10 LOOP
        v_idx := MOD(i, 4) + 1; -- 4개의 택배사 중 하나를 선택하도록 분배
        INSERT INTO seller (seq, name, email, phone, zipcode, address, address_detail, joined_at, status, id, password, supply_rate, delivery_seq, account_number)
        VALUES (seller_seq.NEXTVAL, '판매처 ' || i, 'seller' || i || '@test.com', '010-1234-567' || (i-1), '1234' || (i-1), '테스트 도로명 주소 ' || i, '상세 주소 ' || i, SYSDATE, 1, 'seller' || i, '$2a$10$Uo2k.40T25x5D16jS1tC6O3x8mH7x8a9y10i11o12p13q14r15s16', 60 + MOD(i, 3) * 5, v_delivery_ids(v_idx), '123-456-7890' || (i-1))
        RETURNING seq INTO v_seq;
        v_seller_ids(i) := v_seq;
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('판매처 10개 등록 완료.');

    -- 3. 카테고리 (category) 생성 (대분류 3개 -> 각 중분류 2개 -> 각 소분류 3개 = 총 18개 소분류)
    -- 모든 상품이 카테고리의 '소분류'(depth_level=2)를 참조하도록 설계함
    v_idx := 1;
    FOR d0 IN 1..3 LOOP
        -- 대분류 (depth_level = 0)
        INSERT INTO category (seq, category_name, depth_level, parent_seq)
        VALUES (category_seq.NEXTVAL, '대분류 카테고리 ' || d0, 0, NULL)
        RETURNING seq INTO v_parent_seq;
        
        FOR d1 IN 1..2 LOOP
            -- 중분류 (depth_level = 1)
            INSERT INTO category (seq, category_name, depth_level, parent_seq)
            VALUES (category_seq.NEXTVAL, '중분류 ' || d0 || '-' || d1, 1, v_parent_seq)
            RETURNING seq INTO v_sub_parent_seq;
            
            FOR d2 IN 1..3 LOOP
                -- 소분류 (depth_level = 2, 실제 상품들이 참조하게 될 Leaf 노드)
                INSERT INTO category (seq, category_name, depth_level, parent_seq)
                VALUES (category_seq.NEXTVAL, '소분류 ' || d0 || '-' || d1 || '-' || d2, 2, v_sub_parent_seq)
                RETURNING seq INTO v_seq;
                
                v_subcat_ids(v_idx) := v_seq;
                v_idx := v_idx + 1;
            END LOOP;
        END LOOP;
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('대/중/소 카테고리 등록 완료 (소분류 18개 생성).');

    -- 4. 상품 (product) 1050건 등록 및 옵션/이미지 연동
    FOR i IN 1..1050 LOOP
        -- 10개의 판매처와 18개의 소분류 카테고리를 순환하며 자동 분배
        v_seller_seq := v_seller_ids(MOD(i, 10) + 1);
        v_cat_seq := v_subcat_ids(MOD(i, 18) + 1);
        v_price := 10000 + MOD(i, 30) * 3000;
        
        -- 상품 추가
        INSERT INTO product (
            seq, seller_seq, category_seq, product_name, price, content, 
            sale_status, approval_status, hide_yn, view_count, avg_rating, 
            review_count, sales_count, created_date, status
        ) VALUES (
            product_seq.NEXTVAL, v_seller_seq, v_cat_seq, '테스트 상품 ' || i, v_price, 
            '이 상품은 대용량 테스트를 위해 자동으로 생성된 더미 상품 ' || i || ' 입니다.', 
            CASE WHEN MOD(i, 15) = 0 THEN 'SOLD_OUT' ELSE 'ON_SALE' END, -- 15건마다 한 개씩 품절(SOLD_OUT) 상태 부여
            'APPROVED', 'N', MOD(i, 400), ROUND(3.0 + MOD(i, 21) * 0.1, 1), 
            MOD(i, 40), MOD(i, 250), SYSDATE, 'NORMAL'
        ) RETURNING seq INTO v_prod_seq;
        
        -- 상품당 옵션 2개 등록 (블랙 / 화이트)
        INSERT INTO options (
            seq, product_seq, color, options_size, volume_weight, storage_type, 
            options_type, stock, safety_stock, additional_price
        ) VALUES (
            options_seq.NEXTVAL, v_prod_seq, '블랙', 'Free', '150g', '상온', 
            '의류', 100 + MOD(i, 50), 10, 0
        );

        INSERT INTO options (
            seq, product_seq, color, options_size, volume_weight, storage_type, 
            options_type, stock, safety_stock, additional_price
        ) VALUES (
            options_seq.NEXTVAL, v_prod_seq, '화이트', 'Free', '150g', '상온', 
            '의류', 80 + MOD(i, 50), 10, 1500 -- 화이트 옵션은 1,500원 추가금 적용
        );
        
        -- 상품 대표 썸네일 이미지 등록
        INSERT INTO product_image (
            seq, product_seq, image_url, public_id, thumbnail_yn, 
            image_order, created_date, status
        ) VALUES (
            product_image_seq.NEXTVAL, v_prod_seq, 
            'https://res.cloudinary.com/demo/image/upload/v1234567/sample_product_' || MOD(i, 10) || '.png', 
            'sample_product_' || MOD(i, 10), 'Y', 1, SYSDATE, 'NORMAL'
        );
    END LOOP;

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('총 1,050건의 상품과 2,100건의 옵션, 1,050건의 상품 이미지 데이터 등록이 성공적으로 완료되었습니다.');

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('데이터 생성 중 오류가 발생하였습니다: ' || SQLERRM);
        RAISE;
END;
/
