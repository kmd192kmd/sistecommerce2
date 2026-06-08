-- 1. 본사허브 (서울 대치동 쌍용교육센터 - H타워 9층)
INSERT INTO hub (seq, name, zip_code, road_address, detail_address, latitude, longitude)
VALUES (hub_seq.NEXTVAL, '본사허브', '06193', '서울특별시 강남구 테헤란로70길 12', 'H타워 9층', 37.5049, 127.0505);

-- 2. 중간허브 1 (경기 - 수원시 팔달구 효원로 241)
INSERT INTO hub (seq, name, zip_code, road_address, detail_address, latitude, longitude)
VALUES (hub_seq.NEXTVAL, '중간허브1(경기)', '16490', '경기도 수원시 팔달구 효원로 241', NULL, 37.2635, 127.0286);

-- 3. 중간허브 2 (충청 - 대전광역시 서구 둔산로 100)
INSERT INTO hub (seq, name, zip_code, road_address, detail_address, latitude, longitude)
VALUES (hub_seq.NEXTVAL, '중간허브2(충청)', '35242', '대전광역시 서구 둔산로 100', NULL, 36.3504, 127.3845);

-- 4. 중간허브 3 (경상 - 대구광역시 북구 연암로 40)
INSERT INTO hub (seq, name, zip_code, road_address, detail_address, latitude, longitude)
VALUES (hub_seq.NEXTVAL, '중간허브3(경상)', '41542', '대구광역시 북구 연암로 40', NULL, 35.8714, 128.6014);

-- 5. 변경사항 커밋
COMMIT;


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

        -- 3. 카테고리 (category) 생성 (요구분석서 기준)
    -- 모든 상품이 카테고리의 '소분류'(depth_level=2)를 참조하도록 설계함
    v_idx := 1;

    -- ==========================================
    -- 대분류: 식품
    -- ==========================================
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '식품', 0, NULL) RETURNING seq INTO v_parent_seq;
    -- 중분류: 유제품
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '유제품', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '요구르트', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '우유', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '치즈', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 육류
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '육류', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '소고기', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '돼지고기', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '닭고기', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 채소
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '채소', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '잎채소', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '뿌리채소', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '열매채소', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 어류
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '어류', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '생선', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '조개/갑각류', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '건어물', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 즉석식품
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '즉석식품', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '밀키트', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '즉석밥', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '냉동식품', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '라면', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 음료
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '음료', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '탄산음료', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '주스', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '커피', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '차', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '생수', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;

    -- ==========================================
    -- 대분류: 생활용품
    -- ==========================================
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '생활용품', 0, NULL) RETURNING seq INTO v_parent_seq;
    -- 중분류: 주방용품
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '주방용품', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '조리도구', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '식기/컵', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '냄비/프라이팬', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 청소용품
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '청소용품', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '청소도구', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '걸레/청소포', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '쓰레기통', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 수납용품
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '수납용품', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '리빙박스', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '바구니/정리함', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '옷걸이/행거', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 욕실용품
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '욕실용품', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '샤워용품', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '수건/타월', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '욕실매트', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 세정용품
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '세정용품', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '주방세제', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '세탁세제', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '욕실세정제', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;

    -- ==========================================
    -- 대분류: 의류
    -- ==========================================
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '의류', 0, NULL) RETURNING seq INTO v_parent_seq;
    -- 중분류: 상의
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '상의', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '티셔츠', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '맨투맨/후드티', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '셔츠', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '아우터', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 하의
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '하의', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '면바지', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '청바지', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '반바지', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '치마', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 신발
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '신발', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '구두', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '슬리퍼/샌들', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '운동화', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 잡화
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '잡화', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '모자', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '목도리', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '장갑', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;

    -- ==========================================
    -- 대분류: 전자제품
    -- ==========================================
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '전자제품', 0, NULL) RETURNING seq INTO v_parent_seq;
    -- 중분류: 스마트폰 / 태블릿
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '스마트폰 / 태블릿', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '스마트폰', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '태블릿', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '스마트워치', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 컴퓨터
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '컴퓨터', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '노트북', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '데스크탑 완본체', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: PC 주변기기
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, 'PC 주변기기', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '마우스', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '키보드', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '헤드폰', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '이어폰', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    -- 중분류: 콘솔/영상
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '콘솔/영상', 1, v_parent_seq) RETURNING seq INTO v_sub_parent_seq;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '게임 콘솔', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    INSERT INTO category (seq, category_name, depth_level, parent_seq) VALUES (category_seq.NEXTVAL, '카메라', 2, v_sub_parent_seq) RETURNING seq INTO v_seq;
    v_subcat_ids(v_idx) := v_seq;
    v_idx := v_idx + 1;
    DBMS_OUTPUT.PUT_LINE('대/중/소 카테고리 등록 완료 (소분류 ' || (v_idx - 1) || '개 생성).');

    -- 4. 상품 (product) 1050건 등록 및 옵션/이미지 연동
    FOR i IN 1..1050 LOOP
        -- 10개의 판매처와 18개의 소분류 카테고리를 순환하며 자동 분배
        v_seller_seq := v_seller_ids(MOD(i, 10) + 1);
        v_cat_seq := v_subcat_ids(MOD(i, v_idx - 1) + 1);
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
