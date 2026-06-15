----------------------------------------------------------------------------------------------------------------------------------------
-- 기본 테이블 DDL
----------------------------------------------------------------------------------------------------------------------------------------



-- ==========================================
-- [ 9차 의존 테이블 삭제 ]
-- ==========================================
DROP TABLE delivery_history CASCADE CONSTRAINTS;
DROP TABLE refund CASCADE CONSTRAINTS;
DROP TABLE virtual_account CASCADE CONSTRAINTS;

-- ==========================================
-- [ 8차 의존 테이블 삭제 ]
-- ==========================================
DROP TABLE delivery CASCADE CONSTRAINTS;
DROP TABLE review_image CASCADE CONSTRAINTS;
DROP TABLE returns CASCADE CONSTRAINTS;
DROP TABLE payment CASCADE CONSTRAINTS;

-- ==========================================
-- [ 7차 의존 테이블 삭제 ]
-- ==========================================
DROP TABLE review CASCADE CONSTRAINTS;
DROP TABLE return_request CASCADE CONSTRAINTS;
DROP TABLE orders CASCADE CONSTRAINTS;

-- ==========================================
-- [ 6차 의존 테이블 삭제 ]
-- ==========================================
DROP TABLE order_item CASCADE CONSTRAINTS;
DROP TABLE admin_payment CASCADE CONSTRAINTS;

-- ==========================================
-- [ 5차 의존 테이블 삭제 ]
-- ==========================================
DROP TABLE participation CASCADE CONSTRAINTS;
DROP TABLE waiting_queue CASCADE CONSTRAINTS;
DROP TABLE purchase_order CASCADE CONSTRAINTS;

-- ==========================================
-- [ 4차 의존 테이블 삭제 ]
-- ==========================================
DROP TABLE hot_deal_product CASCADE CONSTRAINTS;
DROP TABLE group_buy_options CASCADE CONSTRAINTS;
DROP TABLE restock_notification CASCADE CONSTRAINTS;
DROP TABLE cart_log CASCADE CONSTRAINTS;
DROP TABLE cart CASCADE CONSTRAINTS;
DROP TABLE product_wish CASCADE CONSTRAINTS;
DROP TABLE stock_history CASCADE CONSTRAINTS;

-- ==========================================
-- [ 3차 의존 테이블 삭제 ]
-- ==========================================
DROP TABLE group_buy CASCADE CONSTRAINTS;
DROP TABLE options CASCADE CONSTRAINTS;
DROP TABLE product_request CASCADE CONSTRAINTS;
DROP TABLE product_price_analysis CASCADE CONSTRAINTS;
DROP TABLE product_image CASCADE CONSTRAINTS;

-- ==========================================
-- [ 2차 의존 테이블 삭제 ]
-- ==========================================
DROP TABLE product CASCADE CONSTRAINTS;
DROP TABLE memberships_log CASCADE CONSTRAINTS;
DROP TABLE chat_log CASCADE CONSTRAINTS;
DROP TABLE QnA CASCADE CONSTRAINTS;

-- ==========================================
-- [ 1차 의존 테이블 삭제 ]
-- ==========================================
DROP TABLE member_withdrawal CASCADE CONSTRAINTS;
DROP TABLE member_status_log CASCADE CONSTRAINTS;
DROP TABLE delivery_address CASCADE CONSTRAINTS;
DROP TABLE login_log CASCADE CONSTRAINTS;
DROP TABLE member_dormant CASCADE CONSTRAINTS;
DROP TABLE payment_method CASCADE CONSTRAINTS;
DROP TABLE remember_me_token CASCADE CONSTRAINTS;
DROP TABLE notification_preferences CASCADE CONSTRAINTS;
DROP TABLE email_token CASCADE CONSTRAINTS;
DROP TABLE member_coupon CASCADE CONSTRAINTS;
DROP TABLE chat CASCADE CONSTRAINTS;
DROP TABLE memberships CASCADE CONSTRAINTS;
DROP TABLE seller CASCADE CONSTRAINTS;

-- ==========================================
-- [ 독립 테이블 삭제 (Level 0) ]
-- ==========================================
DROP TABLE category CASCADE CONSTRAINTS;
DROP TABLE FAQ CASCADE CONSTRAINTS;
DROP TABLE member CASCADE CONSTRAINTS;
DROP TABLE notification CASCADE CONSTRAINTS;
DROP TABLE withdrawal_reason CASCADE CONSTRAINTS;
DROP TABLE Admin CASCADE CONSTRAINTS;
DROP TABLE refund_reason CASCADE CONSTRAINTS;
DROP TABLE hot_deal CASCADE CONSTRAINTS;
DROP TABLE hub CASCADE CONSTRAINTS;
DROP TABLE memberships_info CASCADE CONSTRAINTS;
DROP TABLE delivery_company CASCADE CONSTRAINTS;
DROP TABLE coupon CASCADE CONSTRAINTS;

-- ==========================================
-- 0. 시퀀스 삭제 (DROP SEQUENCE)
-- 기존 시퀀스가 있을 경우 초기화하기 위해 먼저 삭제합니다.
-- ==========================================
DROP SEQUENCE coupon_seq;
DROP SEQUENCE delivery_company_seq;
DROP SEQUENCE memberships_info_seq;
DROP SEQUENCE hub_seq;
DROP SEQUENCE hot_deal_seq;
DROP SEQUENCE refund_reason_seq;
DROP SEQUENCE Admin_seq;
DROP SEQUENCE withdrawal_reason_seq;
DROP SEQUENCE notification_seq;
DROP SEQUENCE member_seq;
DROP SEQUENCE FAQ_seq;
DROP SEQUENCE category_seq;
DROP SEQUENCE seller_seq;
DROP SEQUENCE memberships_seq;
DROP SEQUENCE chat_seq;
DROP SEQUENCE member_coupon_seq;
DROP SEQUENCE email_token_seq;
DROP SEQUENCE notification_preferences_seq;
DROP SEQUENCE remember_me_token_seq;
DROP SEQUENCE payment_method_seq;
DROP SEQUENCE member_dormant_seq;
DROP SEQUENCE login_log_seq;
DROP SEQUENCE delivery_address_seq;
DROP SEQUENCE member_status_log_seq;
DROP SEQUENCE member_withdrawal_seq;
DROP SEQUENCE QnA_seq;
DROP SEQUENCE chat_log_seq;
DROP SEQUENCE memberships_log_seq;
DROP SEQUENCE product_seq;
DROP SEQUENCE product_image_seq;
DROP SEQUENCE product_price_analysis_seq;
DROP SEQUENCE product_request_seq;
DROP SEQUENCE options_seq;
DROP SEQUENCE group_buy_seq;
DROP SEQUENCE stock_history_seq;
DROP SEQUENCE product_wish_seq;
DROP SEQUENCE cart_seq;
DROP SEQUENCE cart_log_seq;
DROP SEQUENCE restock_notification_seq;
DROP SEQUENCE group_buy_options_seq;
DROP SEQUENCE hot_deal_product_seq;
DROP SEQUENCE purchase_order_seq;
DROP SEQUENCE waiting_queue_seq;
DROP SEQUENCE participation_seq;
DROP SEQUENCE admin_payment_seq;
DROP SEQUENCE order_item_seq;
DROP SEQUENCE orders_seq;
DROP SEQUENCE return_request_seq;
DROP SEQUENCE review_seq;
DROP SEQUENCE payment_seq;
DROP SEQUENCE returns_seq;
DROP SEQUENCE review_image_seq;
DROP SEQUENCE delivery_seq;
DROP SEQUENCE virtual_account_seq;
DROP SEQUENCE refund_seq;
DROP SEQUENCE delivery_history_seq;

-- ==========================================
-- 0. 시퀀스 생성 (CREATE SEQUENCE)
-- ==========================================
CREATE SEQUENCE coupon_seq NOCACHE;
CREATE SEQUENCE delivery_company_seq NOCACHE;
CREATE SEQUENCE memberships_info_seq NOCACHE;
CREATE SEQUENCE hub_seq NOCACHE;
CREATE SEQUENCE hot_deal_seq NOCACHE;
CREATE SEQUENCE refund_reason_seq NOCACHE;
CREATE SEQUENCE Admin_seq NOCACHE;
CREATE SEQUENCE withdrawal_reason_seq NOCACHE;
CREATE SEQUENCE notification_seq NOCACHE;
CREATE SEQUENCE member_seq NOCACHE;
CREATE SEQUENCE FAQ_seq NOCACHE;
CREATE SEQUENCE category_seq NOCACHE;
CREATE SEQUENCE seller_seq NOCACHE;
CREATE SEQUENCE memberships_seq NOCACHE;
CREATE SEQUENCE chat_seq NOCACHE;
CREATE SEQUENCE member_coupon_seq NOCACHE;
CREATE SEQUENCE email_token_seq NOCACHE;
CREATE SEQUENCE notification_preferences_seq NOCACHE;
CREATE SEQUENCE remember_me_token_seq NOCACHE;
CREATE SEQUENCE payment_method_seq NOCACHE;
CREATE SEQUENCE member_dormant_seq NOCACHE;
CREATE SEQUENCE login_log_seq NOCACHE;
CREATE SEQUENCE delivery_address_seq NOCACHE;
CREATE SEQUENCE member_status_log_seq NOCACHE;
CREATE SEQUENCE member_withdrawal_seq NOCACHE;
CREATE SEQUENCE QnA_seq NOCACHE;
CREATE SEQUENCE chat_log_seq NOCACHE;
CREATE SEQUENCE memberships_log_seq NOCACHE;
CREATE SEQUENCE product_seq NOCACHE;
CREATE SEQUENCE product_image_seq NOCACHE;
CREATE SEQUENCE product_price_analysis_seq NOCACHE;
CREATE SEQUENCE product_request_seq NOCACHE;
CREATE SEQUENCE options_seq NOCACHE;
CREATE SEQUENCE group_buy_seq NOCACHE;
CREATE SEQUENCE stock_history_seq NOCACHE;
CREATE SEQUENCE product_wish_seq NOCACHE;
CREATE SEQUENCE cart_seq NOCACHE;
CREATE SEQUENCE cart_log_seq NOCACHE;
CREATE SEQUENCE restock_notification_seq NOCACHE;
CREATE SEQUENCE group_buy_options_seq NOCACHE;
CREATE SEQUENCE hot_deal_product_seq NOCACHE;
CREATE SEQUENCE purchase_order_seq NOCACHE;
CREATE SEQUENCE waiting_queue_seq NOCACHE;
CREATE SEQUENCE participation_seq NOCACHE;
CREATE SEQUENCE admin_payment_seq NOCACHE;
CREATE SEQUENCE order_item_seq NOCACHE;
CREATE SEQUENCE orders_seq NOCACHE;
CREATE SEQUENCE return_request_seq NOCACHE;
CREATE SEQUENCE review_seq NOCACHE;
CREATE SEQUENCE payment_seq NOCACHE;
CREATE SEQUENCE returns_seq NOCACHE;
CREATE SEQUENCE review_image_seq NOCACHE;
CREATE SEQUENCE delivery_seq NOCACHE;
CREATE SEQUENCE virtual_account_seq NOCACHE;
CREATE SEQUENCE refund_seq NOCACHE;
CREATE SEQUENCE delivery_history_seq NOCACHE;

-- ==========================================
-- 1. 의존성이 없는 독립 테이블 생성 (Level 0)
-- ==========================================
CREATE TABLE coupon (
    seq number NOT NULL PRIMARY KEY,
    name varchar2(100) NOT NULL,
    discount_type number NOT NULL,
    start_date date NOT NULL,
    valid_days number NOT NULL,
    status number NOT NULL,
    discount_price number NULL,
    discount_rate number NULL
);

CREATE TABLE delivery_company (
    seq number NOT NULL PRIMARY KEY,
    name varchar2(60) NOT NULL UNIQUE,
    customer_service_phone varchar2(20) NOT NULL UNIQUE,
    base_delivery_fee number NOT NULL,
    monthly_fee number NOT NULL
);

CREATE TABLE memberships_info (
    seq number NOT NULL PRIMARY KEY,
    price number DEFAULT 0 NOT NULL
);

CREATE TABLE hub (
    seq number NOT NULL PRIMARY KEY,
    name varchar2(60) NOT NULL UNIQUE,
    zip_code varchar2(5) NOT NULL,
    road_address varchar2(200) NOT NULL,
    detail_address varchar2(100) NULL,
    latitude number(10,7) NOT NULL,
    longitude number(10,7) NOT NULL
);

CREATE TABLE hot_deal (
    seq number NOT NULL PRIMARY KEY,
    name varchar2(100) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    status number NOT NULL,
    discount_rate number NULL,
    discount_price number NULL
);

CREATE TABLE refund_reason (
    seq NUMBER NOT NULL PRIMARY KEY,
    reason VARCHAR(100) NOT NULL
);

CREATE TABLE Admin (
    seq number NOT NULL PRIMARY KEY,
    id varchar2(30) NOT NULL UNIQUE,
    password varchar2(100) NOT NULL,
    adm_role number DEFAULT 0 NOT NULL,
    adm_status number DEFAULT 0 NOT NULL,
    role varchar2(50) DEFAULT 'ROLE_ADMIN' NOT NULL
);

CREATE TABLE withdrawal_reason (
    seq number NOT NULL PRIMARY KEY,
    reason varchar2(100) NOT NULL
);

CREATE TABLE notification (
    seq number NOT NULL PRIMARY KEY,
    type varchar2(50) NOT NULL,
    title varchar2(50) NOT NULL,
    content varchar2(100) NOT NULL,
    recipient_type varchar2(20) NOT NULL,
    recipient_seq number NULL,
    reference_type varchar2(20) NOT NULL,
    reference_seq number NULL,
    created_at timestamp DEFAULT sysdate NOT NULL,
    read_at timestamp NULL
);

CREATE TABLE member (
    seq number NOT NULL PRIMARY KEY,
    username varchar2(100) NOT NULL UNIQUE,
    password varchar2(100) NULL,
    name varchar2(60) NULL,
    nickname varchar2(60) NOT NULL UNIQUE,
    email varchar2(100) NULL UNIQUE,
    phone varchar2(20) NULL,
    ci varchar2(100) NULL UNIQUE,
    di varchar2(100) NULL,
    zipcode varchar2(5) NULL,
    address varchar2(200) NULL,
    address_detail varchar2(100) NULL,
    gender char(1) NULL,
    birth date NULL,
    status number DEFAULT 1 NOT NULL,
    role varchar2(50) DEFAULT 'ROLE_USER' NOT NULL,
    email_verified char(1) DEFAULT 'N' NULL,
    phone_verified char(1) DEFAULT 'N' NULL,
    two_factor char(1) DEFAULT 'N' NULL,
    totp varchar2(100) NULL,
    pw_changed_at timestamp DEFAULT sysdate NULL,
    last_login_at timestamp NULL,
    withdrawal_requested_at timestamp NULL,
    joined_at timestamp DEFAULT sysdate NULL,
    updated_at timestamp NULL,
    login_type varchar2(10) NOT NULL
);

CREATE TABLE FAQ (
    seq number NOT NULL PRIMARY KEY,
    question clob NOT NULL,
    answer clob NOT NULL,
    read_count number DEFAULT 0 NOT NULL
);

CREATE TABLE category (
    seq number NOT NULL PRIMARY KEY,
    category_name VARCHAR(255) NOT NULL,
    depth_level number NOT NULL,
    parent_seq number NULL,
    FOREIGN KEY (parent_seq) REFERENCES category (seq)
);

-- ==========================================
-- 2. 1차 의존 테이블 생성 (Level 1)
-- ==========================================
CREATE TABLE seller (
    seq number NOT NULL PRIMARY KEY,
    name varchar2(30) NOT NULL,
    email varchar2(100) NULL,
    phone varchar2(20) NULL,
    zipcode varchar2(100) NOT NULL,
    address varchar2(100) NOT NULL,
    address_detail varchar2(100) NULL,
    joined_at timestamp DEFAULT sysdate NOT NULL,
    status number DEFAULT 1 NOT NULL,
    id VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    supply_rate number NOT NULL,
    delivery_company_seq number NOT NULL,
    account_number varchar2(100) NOT NULL,
    FOREIGN KEY (delivery_company_seq) REFERENCES delivery_company (seq)
);

CREATE TABLE memberships (
    seq number NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    billing_key varchar2(200) NOT NULL,
    status varchar2(20) NOT NULL,
    started_at timestamp DEFAULT sysdate NOT NULL,
    expire_at timestamp NOT NULL,
    next_billing_at timestamp NULL,
    canceled_at timestamp NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE chat (
    seq number DEFAULT 0 NOT NULL PRIMARY KEY,
    admin_seq number NOT NULL,
    member_seq number NOT NULL,
    status number DEFAULT 0 NOT NULL,
    created_at timestamp NOT NULL,
    FOREIGN KEY (admin_seq) REFERENCES Admin (seq),
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE member_coupon (
    seq number NOT NULL PRIMARY KEY,
    coupon_seq number NOT NULL,
    member_seq number NOT NULL,
    status number NOT NULL,
    FOREIGN KEY (coupon_seq) REFERENCES coupon (seq),
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE email_token (
    seq number NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    token_hash varchar2(200) NOT NULL UNIQUE,
    purpose varchar2(30) NOT NULL,
    expire_at timestamp NOT NULL,
    used_yn char(1) DEFAULT 'N' NOT NULL,
    created_at timestamp DEFAULT sysdate NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE notification_preferences (
    seq number NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    email_yn char(1) DEFAULT 'N' NOT NULL,
    sms_yn char(1) DEFAULT 'N' NOT NULL,
    push_yn char(1) DEFAULT 'N' NOT NULL,
    marketing_email_yn char(1) DEFAULT 'N' NOT NULL,
    marketing_sms_yn char(1) DEFAULT 'N' NOT NULL,
    updated_at timestamp DEFAULT sysdate NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE remember_me_token (
    seq number NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    token_hash varchar2(200) NOT NULL UNIQUE,
    device_type varchar2(100) NOT NULL,
    device_fingerprint varchar2(200) NULL,
    expire_at timestamp NOT NULL,
    used_yn char(1) DEFAULT 'N' NOT NULL,
    created_at timestamp DEFAULT sysdate NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE payment_method (
    seq number NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    type varchar2(20) NOT NULL,
    billing_key varchar2(200) NOT NULL,
    company varchar2(50) NULL,
    masked_number varchar2(30) NULL,
    method_alias varchar2(30) NULL,
    default_yn char(1) DEFAULT 'N' NOT NULL,
    status char(1) DEFAULT 'Y' NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE member_dormant (
    seq number NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    username varchar2(100) NOT NULL,
    password varchar2(100) NULL,
    name varchar2(60) NULL,
    nickname varchar2(60) NULL,
    email varchar2(100) NULL,
    phone varchar2(20) NULL,
    ci varchar2(100) NULL,
    di varchar2(100) NULL,
    zipcode varchar2(5) NULL,
    address varchar2(200) NULL,
    address_detail varchar2(100) NULL,
    gender char(1) NULL,
    birth date NULL,
    dormant_at timestamp DEFAULT sysdate NOT NULL,
    dormant_reason varchar2(100) NOT NULL,
    auto_dormant_yn char(1) DEFAULT 'Y' NOT NULL,
    scheduled_delete_at date NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE login_log (
    seq number DEFAULT 0 NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    ip_address varchar2(20) NOT NULL,
    device_type varchar2(45) NOT NULL,
    user_agent varchar2(300) NULL,
    result number DEFAULT 0 NOT NULL,
    fail_reason varchar2(50) NULL,
    created_at timestamp DEFAULT sysdate NOT NULL,
    login_type number DEFAULT 0 NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE delivery_address (
    seq number NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    address_alias varchar2(30) NULL,
    recipient_name varchar2(60) NOT NULL,
    recipient_phone varchar2(20) NOT NULL,
    zipcode varchar2(5) NOT NULL,
    address varchar2(200) NOT NULL,
    address_detail varchar2(100) NULL,
    note varchar2(200) NULL,
    entry_code varchar2(20) NULL,
    default_yn char(1) DEFAULT 'N' NOT NULL,
    status char(1) DEFAULT 'Y' NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE member_status_log (
    seq number NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    admin_seq number NULL,
    prev_status number NOT NULL,
    new_status number NOT NULL,
    reason varchar2(200) NULL,
    changed_at timestamp DEFAULT sysdate NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq),
    FOREIGN KEY (admin_seq) REFERENCES Admin (seq)
);

CREATE TABLE member_withdrawal (
    seq number NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    withdrawal_reason_seq number NOT NULL,
    username varchar2(100) NOT NULL,
    password varchar2(100) NULL,
    name varchar2(60) NULL,
    nickname varchar2(60) NULL,
    email varchar2(100) NULL,
    phone varchar2(20) NULL,
    ci varchar2(100) NULL,
    di varchar2(100) NULL,
    zipcode varchar2(5) NULL,
    address varchar2(200) NULL,
    address_detail varchar2(100) NULL,
    gender char(1) NULL,
    birth date NULL,
    withdrwal_requested_at timestamp DEFAULT sysdate NOT NULL,
    withdrawal_completed_at timestamp NULL,
    withdrawal_yn char(1) DEFAULT 'N' NOT NULL,
    scheduled_delete_at date NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq),
    FOREIGN KEY (withdrawal_reason_seq) REFERENCES withdrawal_reason (seq)
);

-- ==========================================
-- 3. 2차 의존 테이블 생성 (Level 2)
-- ==========================================
CREATE TABLE QnA (
    seq number NOT NULL PRIMARY KEY,
    chat_seq number DEFAULT 0 NOT NULL,
    admin_seq number NULL,
    last_ai_response clob NOT NULL,
    status varchar2(20) NOT NULL,
    updated_at timestamp NOT NULL,
    FOREIGN KEY (chat_seq) REFERENCES chat (seq),
    FOREIGN KEY (admin_seq) REFERENCES Admin (seq)
);

CREATE TABLE chat_log (
    seq number DEFAULT 0 NOT NULL PRIMARY KEY,
    chat_seq number DEFAULT 0 NOT NULL,
    sender_type varchar2(20) NOT NULL,
    content varchar2(100) NOT NULL,
    created_at timestamp NOT NULL,
    FOREIGN KEY (chat_seq) REFERENCES chat (seq)
);

CREATE TABLE memberships_log (
    seq number NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    memberships_seq number NOT NULL,
    type varchar2(20) NOT NULL,
    amount number NULL,
    created_at timestamp DEFAULT sysdate NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq),
    FOREIGN KEY (memberships_seq) REFERENCES memberships (seq)
);

CREATE TABLE product (
    seq number NOT NULL PRIMARY KEY,
    seller_seq number NOT NULL,
    category_seq number NOT NULL,
    product_name varchar2(60) NOT NULL,
    price number DEFAULT 0 NOT NULL,
    content clob NOT NULL,
    sale_status varchar2(20) DEFAULT 'ON_SALE' NOT NULL,
    approval_status varchar2(20) DEFAULT 'PENDING' NOT NULL,
    hide_yn char(1) DEFAULT 'N' NOT NULL,
    view_count number DEFAULT 0 NOT NULL,
    avg_rating number(2,1) DEFAULT 0 NOT NULL,
    review_count number DEFAULT 0 NOT NULL,
    sales_count number DEFAULT 0 NOT NULL,
    created_date date DEFAULT sysdate NOT NULL,
    updated_date date NULL,
    status varchar2(20) DEFAULT 'NORMAL' NOT NULL,
    FOREIGN KEY (seller_seq) REFERENCES seller (seq),
    FOREIGN KEY (category_seq) REFERENCES category (seq)
);

-- ==========================================
-- 4. 3차 의존 테이블 생성 (Level 3)
-- ==========================================
CREATE TABLE product_image (
    seq number NOT NULL PRIMARY KEY,
    product_seq number NOT NULL,
    image_url varchar2(1000) NULL,
    public_id varchar2(500) NULL,
    thumbnail_yn char(1) DEFAULT 'N' NOT NULL,
    image_order number DEFAULT 1 NOT NULL,
    created_date date DEFAULT sysdate NOT NULL,
    status varchar2(20) DEFAULT 'NORMAL' NOT NULL,
    FOREIGN KEY (product_seq) REFERENCES product (seq)
);

CREATE TABLE product_price_analysis (
    seq number NOT NULL PRIMARY KEY,
    product_seq number NOT NULL,
    avg_price number DEFAULT 0 NULL,
    min_price number DEFAULT 0 NULL,
    max_price number DEFAULT 0 NULL,
    price_diff number NULL,
    analysis_date date DEFAULT sysdate NOT NULL,
    FOREIGN KEY (product_seq) REFERENCES product (seq)
);

CREATE TABLE product_request (
    seq number NOT NULL PRIMARY KEY,
    product_seq number NOT NULL,
    seller_seq number NOT NULL,
    admin_seq number NOT NULL,
    request_type varchar2(20) NOT NULL,
    request_status varchar2(20) DEFAULT 'PENDING' NULL,
    reject_reason varchar2(1000) NULL,
    request_date date DEFAULT sysdate NULL,
    process_date date NULL,
    FOREIGN KEY (product_seq) REFERENCES product (seq),
    FOREIGN KEY (seller_seq) REFERENCES seller (seq),
    FOREIGN KEY (admin_seq) REFERENCES Admin (seq)
);

-- 테이블명 옵션(options) -> options 로 수정
CREATE TABLE options (
    seq number NOT NULL PRIMARY KEY,
    product_seq number NOT NULL,
    color varchar2(100) NULL,
    options_size varchar2(100) NULL,
    volume_weight varchar2(100) NULL,
    taste varchar2(100) NULL,
    storage_type varchar2(100) NULL,
    scent_ingredient varchar2(100) NULL,
    voltage varchar2(100) NULL,
    quantity_set varchar2(100) NULL,
    size_spec varchar2(100) NULL,
    storage_capacity varchar2(100) NULL,
    memory varchar2(100) NULL,
    switch_axis varchar2(100) NULL,
    connection_type varchar2(100) NULL,
    wearable_spec varchar2(100) NULL,
    material_type varchar2(100) NULL,
    options_type varchar2(100) NULL,
    stock number DEFAULT 0 NOT NULL,
    safety_stock number DEFAULT 0 NOT NULL,
    additional_price number DEFAULT 0 NOT NULL,
    FOREIGN KEY (product_seq) REFERENCES product (seq)
);

CREATE TABLE group_buy (
    seq number NOT NULL PRIMARY KEY,
    product_seq number NOT NULL,
    start_at timestamp NOT NULL,
    end_at timestamp NOT NULL,
    created_at timestamp NOT NULL,
    finished_at timestamp NULL,
    min_count number NOT NULL,
    max_count number NOT NULL,
    original_price number NOT NULL,
    final_price number NOT NULL,
    status varchar2(20) NOT NULL,
    FOREIGN KEY (product_seq) REFERENCES product (seq)
);

-- ==========================================
-- 5. 4차 의존 테이블 생성 (Level 4)
-- ==========================================
CREATE TABLE stock_history (
    seq number NOT NULL PRIMARY KEY,
    type varchar2(60) NOT NULL,
    reason varchar2(60) NOT NULL,
    quantity number NOT NULL,
    before_stock number NOT NULL,
    after_stock number NOT NULL,
    source_seq number NULL,
    source_type varchar2(60) NULL,
    created_at date DEFAULT sysdate NOT NULL,
    options_seq number NOT NULL,
    FOREIGN KEY (options_seq) REFERENCES options (seq)
);

CREATE TABLE product_wish (
    seq number NOT NULL PRIMARY KEY,
    options_seq number NOT NULL,
    member_seq number NOT NULL,
    created_date date DEFAULT sysdate NULL,
    status number DEFAULT 0 NULL,
    FOREIGN KEY (options_seq) REFERENCES options (seq),
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE cart (
    seq number NOT NULL PRIMARY KEY,
    options_seq number NOT NULL,
    member_seq number NOT NULL,
    quantity number DEFAULT 1 NOT NULL,
    created_date date DEFAULT sysdate NOT NULL,
    update_date date DEFAULT sysdate NULL,
    FOREIGN KEY (options_seq) REFERENCES options (seq),
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE cart_log (
    seq number NOT NULL PRIMARY KEY,
    options_seq number NOT NULL,
    member_seq number NOT NULL,
    status varchar2(20) NOT NULL,
    action_date timestamp DEFAULT sysdate NOT NULL,
    FOREIGN KEY (options_seq) REFERENCES options (seq),
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE restock_notification (
    seq number NOT NULL PRIMARY KEY,
    options_seq number NOT NULL,
    member_seq number NOT NULL,
    created_date date DEFAULT sysdate NOT NULL,
    FOREIGN KEY (options_seq) REFERENCES options (seq),
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE group_buy_options (
    seq number NOT NULL PRIMARY KEY,
    group_buy_seq number NOT NULL,
    options_seq number NOT NULL,
    order_qty number NOT NULL,
    occupied_count number DEFAULT 0 NOT NULL,
    FOREIGN KEY (group_buy_seq) REFERENCES group_buy (seq),
    FOREIGN KEY (options_seq) REFERENCES options (seq)
);

CREATE TABLE hot_deal_product (
    seq number NOT NULL PRIMARY KEY,
    hot_deal_seq number NOT NULL,
    options_seq number NOT NULL,
    hot_deal_stock number DEFAULT 0 NULL,
    sold_quantity number DEFAULT 0 NULL,
    FOREIGN KEY (hot_deal_seq) REFERENCES hot_deal (seq),
    FOREIGN KEY (options_seq) REFERENCES options (seq)
);

-- ==========================================
-- 6. 5차 의존 테이블 생성 (Level 5)
-- ==========================================
CREATE TABLE purchase_order (
    seq number NOT NULL PRIMARY KEY,
    status varchar2(60) DEFAULT '발주요청' NOT NULL,
    quantity number NOT NULL,
    supply_price number DEFAULT 0 NOT NULL,
    total_price number DEFAULT 0 NOT NULL,
    order_date date DEFAULT sysdate NOT NULL,
    expected_date date NOT NULL,
    received_date date NULL,
    type varchar2(60) DEFAULT '일반' NOT NULL,
    options_seq number NOT NULL,
    group_buy_options_seq number NULL,
    FOREIGN KEY (options_seq) REFERENCES options (seq),
    FOREIGN KEY (group_buy_options_seq) REFERENCES group_buy_options (seq)
);

CREATE TABLE waiting_queue (
    seq number NOT NULL PRIMARY KEY,
    group_buy_seq number NOT NULL,
    group_buy_options_seq number NOT NULL,
    member_seq number NOT NULL,
    created_at timestamp NOT NULL,
    FOREIGN KEY (group_buy_seq) REFERENCES group_buy (seq),
    FOREIGN KEY (group_buy_options_seq) REFERENCES group_buy_options (seq),
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

CREATE TABLE participation (
    seq number NOT NULL PRIMARY KEY,
    group_buy_seq number NOT NULL,
    group_buy_options_seq number NOT NULL,
    member_seq number NOT NULL,
    status varchar2(20) NOT NULL,
    payment_deadline timestamp NULL,
    promoted_at timestamp NULL,
    created_at timestamp NOT NULL,
    FOREIGN KEY (group_buy_seq) REFERENCES group_buy (seq),
    FOREIGN KEY (group_buy_options_seq) REFERENCES group_buy_options (seq),
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

-- ==========================================
-- 7. 6차 의존 테이블 생성 (Level 6)
-- ==========================================
CREATE TABLE admin_payment (
    seq number DEFAULT 0 NOT NULL PRIMARY KEY,
    purchase_order_seq number NOT NULL,
    status number DEFAULT 0 NOT NULL,
    FOREIGN KEY (purchase_order_seq) REFERENCES purchase_order (seq)
);

CREATE TABLE order_item (
    seq NUMBER NOT NULL PRIMARY KEY,
    participation_seq number NULL,
    options_seq number NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity NUMBER DEFAULT 1 NOT NULL,
    original_price NUMBER NOT NULL,
    hotdeal_discount NUMBER DEFAULT 0 NOT NULL,
    coupon_discount NUMBER DEFAULT 0 NOT NULL,
    participation_discount NUMBER DEFAULT 0 NOT NULL,
    final_price NUMBER NOT NULL,
    sub_total_price NUMBER NOT NULL,
    refund_quantity NUMBER DEFAULT 0 NOT NULL,
    refund_price NUMBER DEFAULT 0 NOT NULL,
    item_status NUMBER DEFAULT 0 NOT NULL,
    return_quantity NUMBER NULL,
    FOREIGN KEY (participation_seq) REFERENCES participation (seq),
    FOREIGN KEY (options_seq) REFERENCES options (seq)
);

-- ==========================================
-- 8. 7차 의존 테이블 생성 (Level 7)
-- ==========================================
CREATE TABLE orders (
    seq NUMBER NOT NULL PRIMARY KEY,
    member_seq number NOT NULL,
    order_item_seq NUMBER NOT NULL,
    member_coupon_seq number NULL,
    order_UID VARCHAR2(100) NOT NULL UNIQUE,
    product_total_price NUMBER NOT NULL,
    coupon_discount NUMBER DEFAULT 0 NOT NULL,
    hotdeal_discount NUMBER DEFAULT 0 NOT NULL,
    field VARCHAR(255) NULL,
    final_price NUMBER NOT NULL,
    total_refund_price NUMBER DEFAULT 0 NOT NULL,
    remain_price NUMBER NOT NULL,
    order_status NUMBER DEFAULT 0 NOT NULL,
    payment_status NUMBER DEFAULT 0 NOT NULL,
    order_date DATE NULL,
    regdate DATE DEFAULT SYSDATE NOT NULL,
    zipcode varchar(5) NOT NULL,
    address varchar(200) NOT NULL,
    address_detail varchar(100) NULL,
    curr_latitude number(10,7) NOT NULL,
    curr_longitude number(10,7) NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq),
    FOREIGN KEY (order_item_seq) REFERENCES order_item (seq),
    FOREIGN KEY (member_coupon_seq) REFERENCES member_coupon (seq)
);

CREATE TABLE return_request (
    seq NUMBER NOT NULL PRIMARY KEY,
    order_item_seq NUMBER NOT NULL,
    refund_reason_seq NUMBER NOT NULL,
    returnUID VARCHAR2(100) NOT NULL UNIQUE,
    return_name VARCHAR2(100) NOT NULL,
    return_quantity NUMBER DEFAULT 1 NOT NULL,
    status NUMBER DEFAULT 0 NOT NULL,
    request_date DATE DEFAULT SYSDATE NOT NULL,
    decision_date DATE NULL,
    completed_date DATE NULL,
    reject_reason VARCHAR2(100) NULL,
    zip_code varchar2(5) NOT NULL,
    road_address varchar2(200) NOT NULL,
    detail_address varchar2(100) NULL,
    FOREIGN KEY (order_item_seq) REFERENCES order_item (seq),
    FOREIGN KEY (refund_reason_seq) REFERENCES refund_reason (seq)
);

CREATE TABLE review (
    seq number NOT NULL PRIMARY KEY,
    product_seq number NOT NULL,
    member_seq number NOT NULL,
    order_item_seq number NOT NULL,
    rating number DEFAULT 0 NOT NULL,
    content clob NOT NULL,
    created_date date DEFAULT sysdate NOT NULL,
    updated_date date NULL,
    status varchar2(20) DEFAULT 'NORMAL' NOT NULL,
    FOREIGN KEY (product_seq) REFERENCES product (seq),
    FOREIGN KEY (member_seq) REFERENCES member (seq),
    FOREIGN KEY (order_item_seq) REFERENCES order_item (seq)
);

-- ==========================================
-- 9. 8차 의존 테이블 생성 (Level 8)
-- ==========================================
CREATE TABLE payment (
    seq NUMBER NOT NULL PRIMARY KEY,
    order_seq NUMBER NOT NULL,
    payment_UID VARCHAR2(100) NOT NULL UNIQUE,
    external_payment_ID VARCHAR2(200) NULL,
    pg_TID VARCHAR2(100) NULL,
    payment_method NUMBER NOT NULL,
    pg_provider VARCHAR2(50) NOT NULL,
    status NUMBER DEFAULT 0 NOT NULL,
    amount NUMBER NOT NULL,
    request_date DATE DEFAULT SYSDATE NOT NULL,
    pay_date DATE NULL,
    fail_reason VARCHAR2(500) NULL,
    cancel_reason VARCHAR2(500) NULL,
    refund_reason VARCHAR2(500) NULL,
    receipt_URL VARCHAR2(100) NULL,
    update_date DATE NULL,
    cancel_date DATE NULL,
    refund_date DATE NULL,
    expire_date DATE NULL,
    FOREIGN KEY (order_seq) REFERENCES orders (seq)
);

CREATE TABLE returns (
    seq number NOT NULL PRIMARY KEY,
    return_request_seq number NOT NULL,
    delivery_company_seq number NOT NULL,
    status varchar2(20) NOT NULL,
    completed_date date NULL,
    tracking_number varchar2(100) NOT NULL UNIQUE,
    FOREIGN KEY (return_request_seq) REFERENCES return_request (seq),
    FOREIGN KEY (delivery_company_seq) REFERENCES delivery_company (seq)
);

CREATE TABLE review_image (
    seq number NOT NULL PRIMARY KEY,
    review_seq number NOT NULL,
    image_url varchar2(1000) NOT NULL,
    public_id varchar2(500) NULL,
    image_order number DEFAULT 1 NOT NULL,
    file_type varchar2(20) NULL,
    file_size number NULL,
    created_date date DEFAULT sysdate NOT NULL,
    updated_date date NULL,
    status varchar2(20) DEFAULT 'NORMAL' NOT NULL,
    FOREIGN KEY (review_seq) REFERENCES review (seq)
);

CREATE TABLE delivery (
    seq number NOT NULL PRIMARY KEY,
    tracking_number varchar2(100) NOT NULL UNIQUE,
    recipient_name varchar2(60) NOT NULL,
    recipient_phone varchar2(20) NOT NULL,
    status varchar2(20) NOT NULL,
    request_memo varchar2(255) NULL,
    dispatch_at date NULL,
    estimated_date date NULL,
    completed_at date NULL,
    distance_surcharge number DEFAULT 0 NOT NULL,
    total_delivery_fee number NOT NULL,
    delivery_company_seq number NOT NULL,
    orders_seq number NULL,
    purchase_order_seq number NULL,
    FOREIGN KEY (delivery_company_seq) REFERENCES delivery_company (seq),
    FOREIGN KEY (orders_seq) REFERENCES orders (seq),
    FOREIGN KEY (purchase_order_seq) REFERENCES purchase_order (seq)
);

-- ==========================================
-- 10. 9차 의존 테이블 생성 (Level 9)
-- ==========================================
CREATE TABLE virtual_account (
    seq NUMBER NOT NULL PRIMARY KEY,
    payment_seq NUMBER NOT NULL,
    bank_name VARCHAR2(50) NOT NULL,
    account_number VARCHAR2(100) NOT NULL,
    holder_name VARCHAR2(100) NOT NULL,
    depositor_name VARCHAR2(100) NOT NULL,
    deposit_amount NUMBER NOT NULL,
    deadline DATE NOT NULL,
    deposit_date DATE NULL,
    status NUMBER NOT NULL,
    regdate DATE DEFAULT SYSDATE NOT NULL,
    update_date DATE NULL,
    FOREIGN KEY (payment_seq) REFERENCES payment (seq)
);

CREATE TABLE refund (
    seq NUMBER NOT NULL PRIMARY KEY,
    order_item_seq NUMBER NOT NULL,
    payment_seq NUMBER NULL,
    return_request NUMBER NOT NULL,
    returns_seq number NOT NULL,
    refund_UID VARCHAR2(100) NOT NULL UNIQUE,
    refund_quantity NUMBER NOT NULL,
    refund_product_price NUMBER NOT NULL,
    refund_hotdeal NUMBER DEFAULT 0 NOT NULL,
    refund_coupon NUMBER DEFAULT 0 NOT NULL,
    refund_participation NUMBER NOT NULL,
    refund_price NUMBER NOT NULL,
    status NUMBER DEFAULT 0 NOT NULL,
    request_date DATE DEFAULT SYSDATE NOT NULL,
    complete_date DATE NULL,
    update_date DATE NULL,
    FOREIGN KEY (order_item_seq) REFERENCES order_item (seq),
    FOREIGN KEY (payment_seq) REFERENCES payment (seq),
    FOREIGN KEY (return_request) REFERENCES return_request (seq),
    FOREIGN KEY (returns_seq) REFERENCES returns (seq)
);

CREATE TABLE delivery_history (
    seq number NOT NULL PRIMARY KEY,
    location varchar2(20) NOT NULL,
    curr_latitude number(10,7) NOT NULL,
    curr_longitude number(10,7) NOT NULL,
    arrived_at date NULL,
    delivery_seq number NOT NULL,
    hub_seq number NULL,
    FOREIGN KEY (delivery_seq) REFERENCES delivery (seq),
    FOREIGN KEY (hub_seq) REFERENCES hub (seq)
);

-- ==========================================
-- 하단: 테이블 코멘트(설명) 삽입부
-- ==========================================

COMMENT ON COLUMN stock_history.type IS 'IN/OUT';
COMMENT ON COLUMN stock_history.reason IS '입고완료/반품 등';
COMMENT ON COLUMN stock_history.before_stock IS '해당이력 전 재고';
COMMENT ON COLUMN stock_history.after_stock IS '해당이력 후 재고';
COMMENT ON COLUMN stock_history.source_seq IS '옵션번호/발주번호/ 관리자번호 등';
COMMENT ON COLUMN stock_history.source_type IS '주문출고/입고완료/관리자 변경 등';
COMMENT ON COLUMN chat_log.sender_type IS 'user, ai, admin';
COMMENT ON COLUMN payment.payment_UID IS 'UNIQUE';
COMMENT ON COLUMN payment.payment_method IS '카드(0)/계좌이체(1)';
COMMENT ON COLUMN payment.pg_provider IS 'ex)TOSS, KG_INICIS, PORTONE 등';
COMMENT ON COLUMN payment.status IS '결제준비(0)/가상계좌입급대기(1)/결제완료(2)/결제실패(3)/결제취소(4)/환불완료(5)/입금기한만료(6)';
COMMENT ON COLUMN product_request.seq IS '상품요청 고유번호';
COMMENT ON COLUMN product_request.product_seq IS '등록 요청 시에는 생성 후 연결 가능';
COMMENT ON COLUMN product_request.seller_seq IS '요청한 판매처';
COMMENT ON COLUMN product_request.admin_seq IS '승인/반려 처리한 관리자';
COMMENT ON COLUMN product_request.request_type IS 'REGISTER / UPDATE';
COMMENT ON COLUMN product_request.request_status IS 'PENDING / APPROVED / REJECTED';
COMMENT ON COLUMN product_request.reject_reason IS '관리자가 반려 시 입력';
COMMENT ON COLUMN product_request.request_date IS '판매처가 요청한 날짜';
COMMENT ON COLUMN product_request.process_date IS '관리자가 승인/반려한 날짜';
COMMENT ON COLUMN member_coupon.status IS '사용 가능(0)  /사용완료(1) / 기간만료(2)';
COMMENT ON COLUMN orders.order_UID IS 'UNIQUE';
COMMENT ON COLUMN orders.order_status IS '주문생성(0)/결제대기(1)/결제완료(2)/상품준비중(3)/부분배송중(4)/배송중(5)/배송완료(6)/부분환불(7)/전체환불(8)/주문취소(9)';
COMMENT ON COLUMN orders.payment_status IS '결제대기(0)/가상계좌입금대기(1)/결제완료(2)/결제실패(3)/부분환불(4)/전체환불(5)/결제취소(6)';
COMMENT ON COLUMN virtual_account.status IS '계좌발급(0)/입금완료(1)/기한만료(2)/계좌취소(3)';
COMMENT ON COLUMN purchase_order.status IS '(발주요청 / 입고완료 / 물품불량/지연입고) 등';
COMMENT ON COLUMN purchase_order.supply_price IS '상품가 * 공급률, 관리자가 수정가능';
COMMENT ON COLUMN purchase_order.total_price IS '공급가 * 발주수량';
COMMENT ON COLUMN purchase_order.expected_date IS '판매처랑 협의했다 치고 관리자가 작성';
COMMENT ON COLUMN purchase_order.received_date IS '실제 입고된 날짜';
COMMENT ON COLUMN purchase_order.type IS '일반발주/공동구매발주';
COMMENT ON COLUMN purchase_order.group_buy_options_seq IS '공구 상품의 어떤 옵션인지 식별';
COMMENT ON COLUMN product_image.seq IS '상품이미지 고유번호';
COMMENT ON COLUMN product_image.product_seq IS '연결된 상품';
COMMENT ON COLUMN product_image.image_url IS 'Cloudinary 이미지 URL';
COMMENT ON COLUMN product_image.public_id IS 'Cloudinary 이미지 삭제/수정 시 사용';
COMMENT ON COLUMN product_image.thumbnail_yn IS 'Y / N';
COMMENT ON COLUMN product_image.image_order IS '이미지 표시 순서';
COMMENT ON COLUMN product_image.created_date IS '이미지 등록일';
COMMENT ON COLUMN product_image.status IS 'NORMAL / DELETED';
COMMENT ON COLUMN email_token.token_hash IS 'unique';
COMMENT ON COLUMN email_token.purpose IS 'EMAIL_VERIFY(30분) / PW_RESET(1시간) / ACCOUNT_UNLOCK(1시간)';
COMMENT ON COLUMN email_token.used_yn IS '1회용, 사용 즉시 Y';
COMMENT ON COLUMN review_image.seq IS '리뷰 이미지 고유번호';
COMMENT ON COLUMN review_image.review_seq IS '연결된 리뷰';
COMMENT ON COLUMN review_image.image_url IS 'Cloudinary 이미지 URL';
COMMENT ON COLUMN review_image.public_id IS 'Cloudinary 이미지 삭제/교체 시 사용';
COMMENT ON COLUMN review_image.image_order IS '리뷰 이미지 표시 순서';
COMMENT ON COLUMN review_image.file_type IS 'JPG / JPEG / PNG / WEBP';
COMMENT ON COLUMN review_image.file_size IS '이미지 용량';
COMMENT ON COLUMN review_image.created_date IS '이미지 등록일';
COMMENT ON COLUMN review_image.updated_date IS '이미지 수정일';
COMMENT ON COLUMN review_image.status IS 'NORMAL / DELETED';
COMMENT ON COLUMN remember_me_token.token_hash IS 'unique, 평문 저장 금지, 해시 저장';
COMMENT ON COLUMN remember_me_token.expire_at IS '발급후 14일';
COMMENT ON COLUMN remember_me_token.used_yn IS '사용즉시 Y 새토큰 발급';
COMMENT ON COLUMN payment_method.type IS 'CARD/ACCOUNT';
COMMENT ON COLUMN payment_method.billing_key IS '토스페이먼츠 빌링키';
COMMENT ON COLUMN payment_method.default_yn IS '회원 결제수단 중 하나만 y';
COMMENT ON COLUMN payment_method.status IS 'Y:활성 N:비활성';
COMMENT ON COLUMN admin_payment.status IS '0: 미처리, 1: 처리';
COMMENT ON COLUMN member_status_log.admin_seq IS '시스템 처리면 NULL, 관리자 처리면 admin_seq';
COMMENT ON COLUMN member_status_log.prev_status IS '1: 활성 2:휴면 3:일시정지 4:탈퇴보류중 5: 탈퇴';
COMMENT ON COLUMN member_status_log.new_status IS '1: 활성 2:휴면 3:일시정지 4:탈퇴보류중 5: 탈퇴';
COMMENT ON COLUMN member_withdrawal.withdrawal_yn IS 'y/n';
COMMENT ON COLUMN coupon.discount_type IS '할인율(0)/ 할인 가격(1)';
COMMENT ON COLUMN coupon.start_date IS '예약';
COMMENT ON COLUMN coupon.valid_days IS '유효기간';
COMMENT ON COLUMN coupon.status IS '배포 대기(0)/ 배포 완료(1)';
COMMENT ON COLUMN coupon.expire_date IS 'expire_date date GENERATED ALWAYS AS (start_date + valid_days) VIRTUAL 자동 계산으로 값 넣기';
COMMENT ON COLUMN delivery_company.name IS 'unique';
COMMENT ON COLUMN delivery_company.customer_service_phone IS 'unique';
COMMENT ON COLUMN restock_notification.seq IS '재입고알림 고유번호';
COMMENT ON COLUMN restock_notification.options_seq IS '옵션 고유번호';
COMMENT ON COLUMN restock_notification.created_date IS '알림 신청일';
COMMENT ON COLUMN participation.group_buy_seq IS '어떤 공동구매인지 식별';
COMMENT ON COLUMN participation.group_buy_options_seq IS '공동 구매 상품의 어떤 옵션인지 식별';
COMMENT ON COLUMN participation.member_seq IS '공동 구매에 참여한 회원';
COMMENT ON COLUMN participation.status IS '결제 대기(PAYMENT_PENDING) / 참여 중(PARTICIPATING) / 참여 확정(CONFIRMED) / 취소(CANCELLED) / 무산(FAILED) / 만료(EXPIRED, 승격 후 결제기한 미결제로 자격 소멸)';
COMMENT ON COLUMN participation.payment_deadline IS '대기열의 첫 번째 사용자가 승격되었을 때 결제할 수 있는 시간(승격시각 이후 24시간), 마감 기한이 도래하면 즉시 만료';
COMMENT ON COLUMN participation.promoted_at IS '대기열의 첫 번째 사용자가 승격된 시각';
COMMENT ON COLUMN participation.created_at IS '참여 또는 승격된 시각';
COMMENT ON COLUMN delivery.tracking_number IS 'unique';
COMMENT ON COLUMN delivery.status IS 'READY/SHIPPING/DELIVERED/FAILED/DELAYED/CANCELED';
COMMENT ON COLUMN delivery.orders_seq IS '주문번호/발주번호는 배타적 check 제약조건이 걸려있음';
COMMENT ON COLUMN delivery.purchase_order_seq IS '주문번호/발주번호는 배타적 check 제약조건이 걸려있음';

-- 옵션 사이즈 코멘트 변경 적용
COMMENT ON COLUMN options.seq IS '옵션 고유번호';
COMMENT ON COLUMN options.product_seq IS '연결된 상품';
COMMENT ON COLUMN options.options_size IS '옷, 신발';
COMMENT ON COLUMN options.volume_weight IS '100g, 500g, 1kg, 500ml, 1.5L';
COMMENT ON COLUMN options.taste IS '매운맛, 치즈맛, 해물맛';
COMMENT ON COLUMN options.storage_type IS '냉동 / 상온';
COMMENT ON COLUMN options.scent_ingredient IS '주방세제, 세탁세제, 욕실세정제';
COMMENT ON COLUMN options.voltage IS '10V';
COMMENT ON COLUMN options.quantity_set IS '1팩, 3팩, 1박스, 10입 세트';
COMMENT ON COLUMN options.size_spec IS '소형, 중형, 대형, 10L, 50L';
COMMENT ON COLUMN options.storage_capacity IS '128GB, 256GB, 512GB, 1TB';
COMMENT ON COLUMN options.memory IS '8GB, 16GB, 32GB, 64GB';
COMMENT ON COLUMN options.switch_axis IS '청축, 갈축, 적축, 저소음적축';
COMMENT ON COLUMN options.connection_type IS '유선/무선';
COMMENT ON COLUMN options.wearable_spec IS '스마트워치 알 크기 옵션, 시계줄 길이 등';
COMMENT ON COLUMN options.material_type IS '조리 도구, 컵, 냄비 등에 대한 재질';
COMMENT ON COLUMN options.options_type IS '닌텐도 게임(디스크 에디션 vs 디지털 에디션)';
COMMENT ON COLUMN options.stock IS '옵션별 재고';
COMMENT ON COLUMN options.safety_stock IS '판매처 최초입력, 관리자 수정가능';
COMMENT ON COLUMN options.additional_price IS '옵션별 추가되는 금액';

COMMENT ON COLUMN hub.name IS 'unique';
COMMENT ON COLUMN group_buy.product_seq IS '상품 고유 번호';
COMMENT ON COLUMN group_buy.start_at IS '공동 구매 시작 시간';
COMMENT ON COLUMN group_buy.end_at IS '공동 구매 마감 기한';
COMMENT ON COLUMN group_buy.created_at IS '공동 구매가 생성된 시간';
COMMENT ON COLUMN group_buy.finished_at IS '최대 인원에 최초로 도달한 시각';
COMMENT ON COLUMN group_buy.min_count IS '공동 구매 진행 확정에 필요한 최소 참여 인원. 미달 시 공동 구매 무산(= 할인 확정 임계값)';
COMMENT ON COLUMN group_buy.max_count IS '공동 구매 참여 신청(=결제)을 받을 수 있는 최대 인원. 최대 인원 이상일 경우 신규 신청은 대기열에 등록';
COMMENT ON COLUMN group_buy.original_price IS '공동 구매 할인이 적용되지 않은 상태의 가격(공동 구매 진행 중 정가가 변동되면 참여자마다 결제하는 가격이 달라지므로 별도 컬럼을 생성)';
COMMENT ON COLUMN group_buy.final_price IS '공동 구매 할인이 적용된 상태의 가격';
COMMENT ON COLUMN group_buy.status IS '진행 예정(SCHEDULED), 진행중(ONGOING), 마감(진행 확정)(CONFIRMED), 마감(무산됨)(FAILED), 중단됨(관리자가 중단함)(STOPPED)';
COMMENT ON COLUMN delivery_address.address_alias IS '별칭 집 회사등';
COMMENT ON COLUMN delivery_address.note IS '정규화 고려?';
COMMENT ON COLUMN delivery_address.default_yn IS '맨처음 생성된 배송지는 기본배송지로 설정 1개의 y만을 가짐';
COMMENT ON COLUMN delivery_address.status IS 'Y:활성 N:비활성';
COMMENT ON COLUMN product_wish.seq IS '찜 고유번호';
COMMENT ON COLUMN product_wish.options_seq IS '옵션 고유번호';
COMMENT ON COLUMN product_wish.member_seq IS '찜한 회원';
COMMENT ON COLUMN product_wish.created_date IS '찜 등록일';
COMMENT ON COLUMN product_wish.status IS 'NORMAL / DELETED';
COMMENT ON COLUMN refund.payment_seq IS 'API에 따라 환불 요청 전에 데이터를 먼저 생성하는 경우도 존재';
COMMENT ON COLUMN refund.refund_UID IS 'UNIQUE';
COMMENT ON COLUMN refund.status IS '환불요청(0)/환불승인(1)/환불거절(2)/환불완료(3)/환불실패(4)';
COMMENT ON COLUMN category.depth_level IS '대분류(0), 중분류(1), 소분류(2)';
COMMENT ON COLUMN product_price_analysis.seq IS '가격분석 고유번호';
COMMENT ON COLUMN product_price_analysis.product_seq IS '분석 대상 상품';
COMMENT ON COLUMN product_price_analysis.avg_price IS '동일 제품군 평균가';
COMMENT ON COLUMN product_price_analysis.min_price IS '동일 제품군 최저가';
COMMENT ON COLUMN product_price_analysis.max_price IS '동일 제품군 최고가';
COMMENT ON COLUMN product_price_analysis.price_diff IS '현재 상품과 평균가 차이';
COMMENT ON COLUMN product_price_analysis.analysis_date IS '분석 기준일';
COMMENT ON COLUMN delivery_history.location IS 'SENDER/HUB/RECEIVER';
COMMENT ON COLUMN member_dormant.dormant_reason IS 'INACTIVE_12M / USER_REQUEST';
COMMENT ON COLUMN member_dormant.auto_dormant_yn IS 'y/n';
COMMENT ON COLUMN member_dormant.scheduled_delete_at IS '전환일 기준 산정(5년)';
COMMENT ON COLUMN memberships_log.type IS 'join/renew/cancel/expire';
COMMENT ON COLUMN hot_deal.status IS '대기(0) / 진행중(1) / 종료(2)';
COMMENT ON COLUMN login_log.result IS '0: 성공, 1: 실패';
COMMENT ON COLUMN login_log.fail_reason IS 'PW_MISSMATCH / ACCOUNT_LOCK';
COMMENT ON COLUMN login_log.login_type IS '0: 일반, 1: SSO 로그인';
COMMENT ON COLUMN returns.status IS 'READY/RETURNING/RETURNED';
COMMENT ON COLUMN returns.tracking_number IS 'unique';
COMMENT ON COLUMN order_item.options_seq IS '옵션 고유번호';
COMMENT ON COLUMN order_item.product_name IS '이후 상품명이 바뀌어도 주문 내역에서는 구매시점 기준 이름으로 유지';
COMMENT ON COLUMN order_item.original_price IS '상품명과 동일';
COMMENT ON COLUMN order_item.final_price IS '상품 하나당 가격';
COMMENT ON COLUMN order_item.sub_total_price IS '상품 * 개수';
COMMENT ON COLUMN order_item.item_status IS '주문완료(0)/상품준비중(1)/배송중(2)/배송완료(3)/부분환불(4)/전체환불(5)/주문취소(6)/반품요청(7)/반품진행중(8)/반품완료(9)';
COMMENT ON COLUMN seller.status IS '1: 활성 2:휴면 3:일시정지 4:탈퇴보류중 5: 탈퇴';
COMMENT ON COLUMN seller.supply_rate IS '판매가 기준 공급가 계산 비율. (예: 100원 상품, 60% → 공급가 60원)';
COMMENT ON COLUMN seller.account_number IS '계좌번호';
COMMENT ON COLUMN return_request.returnUID IS 'UNIQUE';
COMMENT ON COLUMN return_request.status IS '반품요청(0)/승인(1)/반려(2)';
COMMENT ON COLUMN group_buy_options.group_buy_seq IS '어떤 공동구매인지 식별';
COMMENT ON COLUMN group_buy_options.options_seq IS '상품의 어떤 옵션인지 식별';
COMMENT ON COLUMN group_buy_options.order_qty IS '판매처가 관리자와 합의 하에 특정 공동 구매에  제공하기로 한 상품의 개수(예: 아이폰 블랙 300개, 레드 100개, 핑크 20개)';
COMMENT ON COLUMN group_buy_options.occupied_count IS '최대 인원(max_count)을 차지하는  인원 = 정규 참여자 + 결제 대기 인원. 매진 여부를 판단하기 위함.';
COMMENT ON COLUMN QnA.status IS 'AI_ANSWERED, PENDING_ADMIN, ADMIN_COMPLETED,RE_QUESTIONED_TO_AI';
COMMENT ON COLUMN Admin.id IS 'unique';
COMMENT ON COLUMN Admin.password IS 'Argon2';
COMMENT ON COLUMN Admin.adm_role IS '0: 권한 미부여 관리자1: 최고 관리자, 2: cs관리자, 3: 재고관리자, 4:배송 관리자, 5: 인사,회계 관리자';
COMMENT ON COLUMN Admin.adm_status IS '0: 활성화, 1: 비활성화';
COMMENT ON COLUMN Admin.role IS '스프링 시큐리티 요구';
COMMENT ON COLUMN waiting_queue.group_buy_seq IS '어떤 공동구매인지 식별';
COMMENT ON COLUMN waiting_queue.group_buy_options_seq IS '공동 구매 상품의 어떤 옵션에서 대기하는가';
COMMENT ON COLUMN waiting_queue.member_seq IS '대기열에서 대기중인 사용자';
COMMENT ON COLUMN waiting_queue.created_at IS '대기 등록 시각은 FIFO 순서 기준';
COMMENT ON COLUMN chat.status IS '0: 활성화, 1: 비활성화';
COMMENT ON COLUMN notification.type IS 'GROUP_BUY_CONFIRMED / PAYMENT_DONE / RESTOCK';
COMMENT ON COLUMN notification.title IS '알림 제목';
COMMENT ON COLUMN notification.content IS '알림 내용';
COMMENT ON COLUMN notification.recipient_type IS 'MEMBER / ADMIN / SELLER';
COMMENT ON COLUMN notification.recipient_seq IS '예: 몇 번 MEMBER 인지 seq로 파악';
COMMENT ON COLUMN notification.reference_type IS '알림의 출처가 되는 테이블(예: 공동 구매)';
COMMENT ON COLUMN notification.reference_seq IS '출처 행의 PK, 예: 무슨 공동 구매인지 seq(PK)로 파악';
COMMENT ON COLUMN notification.created_at IS '알림이 생성된 시각';
COMMENT ON COLUMN notification.read_at IS '알림을 읽은 시각, NULL 이면 읽지 않았음';
COMMENT ON COLUMN member.username IS 'unique, 변경불가';
COMMENT ON COLUMN member.password IS 'bcrypt/Argon 2해시 / 소셜로그인 한 사람은 비밀번호가 없다?';
COMMENT ON COLUMN member.name IS '휴면시 null 처리';
COMMENT ON COLUMN member.nickname IS 'unique';
COMMENT ON COLUMN member.email IS 'unique, 휴면시 null 처리';
COMMENT ON COLUMN member.phone IS 'E.164포맷, 휴면시 null 처리';
COMMENT ON COLUMN member.ci IS 'unique, 동일인 식별을 위한 고유키';
COMMENT ON COLUMN member.di IS '사이트별로 다르게 발급되는 식별자';
COMMENT ON COLUMN member.address IS '도로명 주소, 휴면시 null 처리';
COMMENT ON COLUMN member.address_detail IS '상세 주소, 휴면시 null 처리';
COMMENT ON COLUMN member.gender IS 'M:남자/F:여자';
COMMENT ON COLUMN member.birth IS '생년월일';
COMMENT ON COLUMN member.status IS '1: 활성 2:휴면 3:일시정지 4:탈퇴보류중 5: 탈퇴';
COMMENT ON COLUMN member.role IS '스프링 시큐리티 요구';
COMMENT ON COLUMN member.email_verified IS 'Y/N';
COMMENT ON COLUMN member.phone_verified IS 'Y/N';
COMMENT ON COLUMN member.two_factor IS 'Y/N';
COMMENT ON COLUMN member.totp IS 'AES-256 양방향 암호화 고려';
COMMENT ON COLUMN member.withdrawal_requested_at IS '탈퇴 신청일시(3일 유예 기준)';
COMMENT ON COLUMN member.joined_at IS '변경불가';
COMMENT ON COLUMN member.updated_at IS '정보 수정 일시';
COMMENT ON COLUMN member.login_type IS 'LOCAL/KAKAO/NAVER/GOOGLE';
COMMENT ON COLUMN review.seq IS '리뷰 고유번호';
COMMENT ON COLUMN review.product_seq IS '리뷰가 달린 상품';
COMMENT ON COLUMN review.member_seq IS '리뷰 작성자';
COMMENT ON COLUMN review.order_item_seq IS '구매/배송 완료 확인용';
COMMENT ON COLUMN review.rating IS '1 / 2 / 3 / 4 / 5';
COMMENT ON COLUMN review.content IS '리뷰 내용';
COMMENT ON COLUMN review.created_date IS '리뷰 작성일';
COMMENT ON COLUMN review.updated_date IS '리뷰 수정일';
COMMENT ON COLUMN review.status IS 'NORMAL / DELETED / HIDDEN';
COMMENT ON COLUMN product.seq IS '상품 고유번호';
COMMENT ON COLUMN product.seller_seq IS '상품을 등록한 판매처';
COMMENT ON COLUMN product.category_seq IS '상품 카테고리';
COMMENT ON COLUMN product.product_name IS '최소 2자, 최대 60자';
COMMENT ON COLUMN product.sale_status IS 'ON_SALE / SOLD_OUT / STOPPED';
COMMENT ON COLUMN product.approval_status IS 'PENDING / APPROVED / REJECTED';
COMMENT ON COLUMN product.hide_yn IS 'Y / N';
COMMENT ON COLUMN product.view_count IS '상세 조회 시 증가';
COMMENT ON COLUMN product.avg_rating IS '리뷰 평균 별점';
COMMENT ON COLUMN product.review_count IS '리뷰 개수';
COMMENT ON COLUMN product.sales_count IS '판매량 정렬용';
COMMENT ON COLUMN product.created_date IS '등록일';
COMMENT ON COLUMN product.updated_date IS '수정일';
COMMENT ON COLUMN product.status IS 'NORMAL / DELETED';
COMMENT ON COLUMN cart_log.status IS '0: 담기, 1: 삭제 2:구매';
COMMENT ON COLUMN memberships.status IS 'active/canceled/expired/none';


ALTER TABLE memberships MODIFY (
    billing_key varchar2(200) NULL,
    started_at  timestamp NULL,
    expire_at   timestamp NULL
);

alter table cart Drop column update_date;

ALTER TABLE login_log MODIFY ip_address VARCHAR2(45);

ALTER TABLE member_withdrawal
  RENAME COLUMN withdrwal_requested_at TO withdrawal_requested_at;


INSERT INTO withdrawal_reason (seq, reason)
VALUES (withdrawal_reason_seq.NEXTVAL, '서비스를 잘 이용하지 않음');
INSERT INTO withdrawal_reason (seq, reason)
VALUES (withdrawal_reason_seq.NEXTVAL, '상품/서비스에 만족하지 못함');
INSERT INTO withdrawal_reason (seq, reason)
VALUES (withdrawal_reason_seq.NEXTVAL, '개인정보 유출이 우려됨');
INSERT INTO withdrawal_reason (seq, reason)
VALUES (withdrawal_reason_seq.NEXTVAL, '다른 서비스를 이용하려고 함');
INSERT INTO withdrawal_reason (seq, reason)
VALUES (withdrawal_reason_seq.NEXTVAL, '기타');

COMMIT;



----------------------------------------------------------------------------------------------------------------------------------------
-- 김민도 수정사항
----------------------------------------------------------------------------------------------------------------------------------------


--배송테이블 지연시간 컬럼 추가
ALTER TABLE delivery ADD (delay_hours number DEFAULT 0 NOT NULL);

--상품 목록 조회 속도 개선을 위해 상품테이블에 대표이미지 컬럼을 추가
ALTER TABLE product ADD (thumbnail_url VARCHAR2(1000));

--매번 공휴일 api를 불러오지 않고 테이블에 저장하기 위해 공휴일 테이블을 생성
CREATE TABLE holiday (
        holiday_date DATE PRIMARY KEY,
        name VARCHAR2(255)
    );

--컬럼명 오기입으로 인하여 수정함
ALTER TABLE seller RENAME COLUMN delivery_seq TO delivery_company_seq;

-- member_session 테이블 (플랫폼별 세션 관리)
-- platform: PC / ANDROID / IOS
-- 동일 플랫폼으로 새 로그인 시 기존 레코드 삭제 후 신규 등록

CREATE SEQUENCE member_session_seq NOCACHE;

CREATE TABLE member_session (
    seq         number          NOT NULL PRIMARY KEY,
    member_seq  number          NOT NULL,
    session_id  varchar2(200)   NOT NULL UNIQUE,
    platform    varchar2(10)    NOT NULL,
    created_at  timestamp       DEFAULT sysdate NOT NULL,
    FOREIGN KEY (member_seq) REFERENCES member (seq)
);

----------------------------------------------------------------------------------------------------------------------------------------
-- 김민도 제약조건
----------------------------------------------------------------------------------------------------------------------------------------
--배송
ALTER TABLE delivery
ADD CONSTRAINT ck_delivery_target
CHECK (
    (orders_seq IS NOT NULL AND purchase_order_seq IS NULL)
 OR (orders_seq IS NULL AND purchase_order_seq IS NOT NULL)
);

ALTER TABLE delivery
ADD CONSTRAINT ck_delivery_status
CHECK (
    status IN (
        'READY',
        'SHIPPING',
        'DELIVERED',
        'FAILED',
        'DELAYED',
        'CANCELED'
    )
);

ALTER TABLE delivery
ADD CONSTRAINT ck_delivery_fee
CHECK (
    distance_surcharge >= 0
    AND total_delivery_fee >= 0
);

--배송기록
ALTER TABLE delivery_history
ADD CONSTRAINT ck_delivery_history_lat
CHECK (
    curr_latitude BETWEEN -90 AND 90
);

ALTER TABLE delivery_history
ADD CONSTRAINT ck_delivery_history_lng
CHECK (
    curr_longitude BETWEEN -180 AND 180
);

--물류허브
ALTER TABLE hub
ADD CONSTRAINT ck_hub_lat
CHECK (
    latitude BETWEEN -90 AND 90
);

ALTER TABLE hub
ADD CONSTRAINT ck_hub_lng
CHECK (
    longitude BETWEEN -180 AND 180
);

--택배사
ALTER TABLE delivery_company
ADD CONSTRAINT ck_delivery_company_fee
CHECK (
    base_delivery_fee >= 0
    AND monthly_fee >= 0
);

--반품신청
ALTER TABLE return_request
ADD CONSTRAINT ck_return_request_status
CHECK (
    status IN (
        0, -- 신청
        1, -- 승인
        2, -- 수거중
        3, -- 완료
        4  -- 거절
    )
);

ALTER TABLE return_request
ADD CONSTRAINT ck_return_request_qty
CHECK (
    return_quantity > 0
);

--반품
ALTER TABLE returns
ADD CONSTRAINT ck_returns_status
CHECK (
    status IN (
        'READY',
        'RETURNING',
        'RETURNED'
    )
);

--카테고리
ALTER TABLE category
ADD CONSTRAINT ck_category_depth
CHECK (
    depth_level IN (0,1,2)
);

ALTER TABLE category
ADD CONSTRAINT uq_category_name
UNIQUE(parent_seq, category_name);


----------------------------------------------------------------------------------------------------------------------------------------
-- 이세빈 제약조건
----------------------------------------------------------------------------------------------------------------------------------------

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


----------------------------------------------------------------------------------------------------------------------------------------
-- 이찬희 제약조건
----------------------------------------------------------------------------------------------------------------------------------------

-- 발주 테이블 (purchase_order)
ALTER TABLE purchase_order
ADD (
    CONSTRAINT chk_purchase_order_status
    CHECK (status IN ('발주요청', '입고완료', '물품불량', '입고지연', '지연입고')),

    CONSTRAINT chk_purchase_order_quantity
    CHECK (quantity >= 1),

    CONSTRAINT chk_purchase_order_type
    CHECK (type IN ('일반', '공동구매'))
);

-- 재고이력 테이블 (stock_history)
ALTER TABLE stock_history
ADD (
    CONSTRAINT chk_stock_history_type
        CHECK (type IN ('IN', 'OUT')),

    CONSTRAINT chk_stock_history_quantity
        CHECK (quantity >= 1),

    CONSTRAINT chk_stock_history_before_stock
        CHECK (before_stock >= 0),

    CONSTRAINT chk_stock_history_after_stock
        CHECK (after_stock >= 0),

    CONSTRAINT chk_stock_history_source_type
        CHECK (source_type IN ('주문', '발주', '반품', '관리자'))
);

-- 판매처 테이블 (seller)
ALTER TABLE seller
ADD (
    CONSTRAINT chk_seller_supply_rate
        CHECK (supply_rate BETWEEN 1 AND 100)
);


----------------------------------------------------------------------------------------------------------------------------------------
-- 홍태훈 제약조건
----------------------------------------------------------------------------------------------------------------------------------------

-- 1. cart 테이블: 중복 상품 담기 방지
ALTER TABLE cart ADD CONSTRAINT UQ_CART_MEMBER_OPTION UNIQUE (member_seq, options_seq);

-- 2. product_wish 테이블: 중복 찜하기 방지
ALTER TABLE product_wish ADD CONSTRAINT UQ_WISH_MEMBER_OPTION UNIQUE (member_seq, options_seq);

-- 3. coupon 테이블: 만료일 가상 컬럼(Virtual) 추가
ALTER TABLE coupon ADD "expire_date" date GENERATED ALWAYS AS (start_date + valid_days) VIRTUAL;

-- 4. coupon 테이블: (할인가격, 할인율) 둘 중 하나 필수 체크
ALTER TABLE coupon ADD CONSTRAINT CHK_COUPON_DISCOUNT CHECK (discount_rate IS NOT NULL OR discount_price IS NOT NULL);

-- 5. hot_deal 테이블: (할인가격, 할인율) 둘 중 하나 필수 체크
ALTER TABLE hot_deal ADD CONSTRAINT CHK_HOTDEAL_DISCOUNT CHECK (discount_rate IS NOT NULL OR discount_price IS NOT NULL);

----------------------------------------------------------------------------------------------------------------------------------------
-- 황윤재 제약조건
----------------------------------------------------------------------------------------------------------------------------------------

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



----------------------------------------------------------------------------------------------------------------------------------------
-- 곽정도 수정사항
----------------------------------------------------------------------------------------------------------------------------------------
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


