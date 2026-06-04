-- 상품
-- 판매상태: 판매중 / 품절 / 판매중지
ALTER TABLE product
ADD CONSTRAINT chk_product_sale_status
CHECK (sale_status IN ('ON_SALE', 'SOLD_OUT', 'STOPPED'));

-- 승인상태: 대기 / 승인 / 미승인
ALTER TABLE product
ADD CONSTRAINT chk_product_approval_status
CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- 숨김여부: Y / N
ALTER TABLE product
ADD CONSTRAINT chk_product_hide_yn
CHECK (hide_yn IN ('Y', 'N'));

-- 상품 상태: 정상 / 삭제
ALTER TABLE product
ADD CONSTRAINT chk_product_status
CHECK (status IN ('NORMAL', 'DELETED'));

-- 상품가격: 0 이상
ALTER TABLE product
ADD CONSTRAINT chk_product_price
CHECK (price >= 0);

-- 조회수: 0 이상
ALTER TABLE product
ADD CONSTRAINT chk_product_view_count
CHECK (view_count >= 0);

-- 평균평점: 0점 이상 5점 이하
ALTER TABLE product
ADD CONSTRAINT chk_product_avg_rating
CHECK (avg_rating BETWEEN 0 AND 5);

-- 리뷰개수: 0 이상
ALTER TABLE product
ADD CONSTRAINT chk_product_review_count
CHECK (review_count >= 0);

-- 판매량: 0 이상
ALTER TABLE product
ADD CONSTRAINT chk_product_sales_count
CHECK (sales_count >= 0);


--옵션
-- 옵션번호는 1 이상
ALTER TABLE options
ADD CONSTRAINT chk_options_seq
CHECK (seq > 0);

-- 연결 상품번호는 1 이상
ALTER TABLE options
ADD CONSTRAINT chk_options_product_seq
CHECK (product_seq > 0);

-- 재고는 0 이상
ALTER TABLE options
ADD CONSTRAINT chk_options_stock
CHECK (stock >= 0);

-- 안전재고는 0 이상
ALTER TABLE options
ADD CONSTRAINT chk_options_safety_stock
CHECK (safety_stock >= 0);

-- 추가금액은 0 이상
ALTER TABLE options
ADD CONSTRAINT chk_options_additional_price
CHECK (additional_price >= 0);


--상품요청
-- 상품요청번호는 1 이상
ALTER TABLE product_request
ADD CONSTRAINT chk_product_request_seq
CHECK (seq > 0);

-- 상품번호는 1 이상
ALTER TABLE product_request
ADD CONSTRAINT chk_product_request_product_seq
CHECK (product_seq > 0);

-- 판매처번호는 1 이상
ALTER TABLE product_request
ADD CONSTRAINT chk_product_request_seller_seq
CHECK (seller_seq > 0);

-- 관리자번호는 1 이상
ALTER TABLE product_request
ADD CONSTRAINT chk_product_request_admin_seq
CHECK (admin_seq > 0);

-- 요청구분: 등록 / 수정
ALTER TABLE product_request
ADD CONSTRAINT chk_product_request_type
CHECK (request_type IN ('REGISTER', 'UPDATE'));

-- 요청상태: 대기 / 승인 / 미승인
ALTER TABLE product_request
ADD CONSTRAINT chk_product_request_status
CHECK (request_status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- 처리일은 요청일보다 빠를 수 없음
ALTER TABLE product_request
ADD CONSTRAINT chk_product_request_process_date
CHECK (process_date IS NULL OR process_date >= request_date);


--리뷰
-- 리뷰번호는 1 이상
ALTER TABLE review
ADD CONSTRAINT chk_review_seq
CHECK (seq > 0);

-- 상품번호는 1 이상
ALTER TABLE review
ADD CONSTRAINT chk_review_product_seq
CHECK (product_seq > 0);

-- 회원번호는 1 이상
ALTER TABLE review
ADD CONSTRAINT chk_review_member_seq
CHECK (member_seq > 0);

-- 주문상품번호는 1 이상
ALTER TABLE review
ADD CONSTRAINT chk_review_order_item_seq
CHECK (order_item_seq > 0);

-- 별점은 1~5만 가능
ALTER TABLE review
ADD CONSTRAINT chk_review_rating
CHECK (rating IN (1, 2, 3, 4, 5));

-- 상태: 정상 / 삭제 / 숨김
ALTER TABLE review
ADD CONSTRAINT chk_review_status
CHECK (status IN ('NORMAL', 'DELETED', 'HIDDEN'));

-- 수정일은 작성일보다 빠를 수 없음
ALTER TABLE review
ADD CONSTRAINT chk_review_updated_date
CHECK (updated_date IS NULL OR updated_date >= created_date);


--리뷰이미지
-- 리뷰이미지번호는 1 이상
ALTER TABLE review_image
ADD CONSTRAINT chk_review_image_seq
CHECK (seq > 0);

-- 리뷰번호는 1 이상
ALTER TABLE review_image
ADD CONSTRAINT chk_review_image_review_seq
CHECK (review_seq > 0);

-- 이미지 순서는 1 이상
ALTER TABLE review_image
ADD CONSTRAINT chk_review_image_order
CHECK (image_order > 0);

-- 파일형식: JPG / JPEG / PNG / WEBP
ALTER TABLE review_image
ADD CONSTRAINT chk_review_image_file_type
CHECK (file_type IN ('JPG', 'JPEG', 'PNG', 'WEBP'));

-- 파일용량은 0 이상
ALTER TABLE review_image
ADD CONSTRAINT chk_review_image_file_size
CHECK (file_size >= 0);

-- 상태: 정상 / 삭제
ALTER TABLE review_image
ADD CONSTRAINT chk_review_image_status
CHECK (status IN ('NORMAL', 'DELETED'));

-- 수정일은 등록일보다 빠를 수 없음
ALTER TABLE review_image
ADD CONSTRAINT chk_review_image_updated_date
CHECK (updated_date IS NULL OR updated_date >= created_date);


--상품이미지
-- 상품이미지번호는 1 이상
ALTER TABLE product_image
ADD CONSTRAINT chk_product_image_seq
CHECK (seq > 0);

-- 상품번호는 1 이상
ALTER TABLE product_image
ADD CONSTRAINT chk_product_image_product_seq
CHECK (product_seq > 0);

-- 대표이미지여부: Y / N
ALTER TABLE product_image
ADD CONSTRAINT chk_product_image_thumbnail_yn
CHECK (thumbnail_yn IN ('Y', 'N'));

-- 이미지 순서는 1 이상
ALTER TABLE product_image
ADD CONSTRAINT chk_product_image_order
CHECK (image_order > 0);

-- 상태: 정상 / 삭제
ALTER TABLE product_image
ADD CONSTRAINT chk_product_image_status
CHECK (status IN ('NORMAL', 'DELETED'));


--가격분석
-- 가격분석번호는 1 이상
ALTER TABLE product_price_analysis
ADD CONSTRAINT chk_price_analysis_seq
CHECK (seq > 0);

-- 상품번호는 1 이상
ALTER TABLE product_price_analysis
ADD CONSTRAINT chk_price_analysis_product_seq
CHECK (product_seq > 0);

-- 평균가격은 0 이상
ALTER TABLE product_price_analysis
ADD CONSTRAINT chk_price_analysis_avg_price
CHECK (avg_price >= 0);

-- 최저가격은 0 이상
ALTER TABLE product_price_analysis
ADD CONSTRAINT chk_price_analysis_min_price
CHECK (min_price >= 0);

-- 최고가격은 0 이상
ALTER TABLE product_price_analysis
ADD CONSTRAINT chk_price_analysis_max_price
CHECK (max_price >= 0);

-- 가격차이는 0 이상
ALTER TABLE product_price_analysis
ADD CONSTRAINT chk_price_analysis_price_diff
CHECK (price_diff >= 0);

-- 최고가격은 최저가격보다 작을 수 없음
ALTER TABLE product_price_analysis
ADD CONSTRAINT chk_price_analysis_min_max
CHECK (
    min_price IS NULL
    OR max_price IS NULL
    OR max_price >= min_price
);

-- 평균가격은 최저가격과 최고가격 사이여야 함
ALTER TABLE product_price_analysis
ADD CONSTRAINT chk_price_analysis_avg_range
CHECK (
    avg_price IS NULL
    OR min_price IS NULL
    OR max_price IS NULL
    OR avg_price BETWEEN min_price AND max_price
);


--찜
-- 찜번호는 1 이상
ALTER TABLE product_wish
ADD CONSTRAINT chk_product_wish_seq
CHECK (seq > 0);

-- 옵션번호는 1 이상
ALTER TABLE product_wish
ADD CONSTRAINT chk_product_wish_options_seq
CHECK (options_seq > 0);

-- 회원번호는 1 이상
ALTER TABLE product_wish
ADD CONSTRAINT chk_product_wish_member_seq
CHECK (member_seq > 0);

ALTER TABLE product_wish
ADD CONSTRAINT chk_product_wish_status
CHECK (status IN ('NORMAL', 'DELETED'));


--재입고알림
-- 재입고알림번호는 1 이상
ALTER TABLE restock_notification
ADD CONSTRAINT chk_restock_notification_seq
CHECK (seq > 0);

-- 옵션번호는 1 이상
ALTER TABLE restock_notification
ADD CONSTRAINT chk_restock_notification_options_seq
CHECK (options_seq > 0);

-- 회원번호는 1 이상
ALTER TABLE restock_notification
ADD CONSTRAINT chk_restock_notification_member_seq
CHECK (member_seq > 0);
