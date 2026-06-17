-- 공구 참여 동시성 테스트 전용 최소 스키마 (Testcontainers Oracle XE init script).
-- 실제 ddl.sql 정의와 동일하되, 락/참여 흐름과 무관한 FK는 생략한다.
-- participate()가 group_buy 상태 확인 → 옵션 점유 → participation INSERT,
-- 옵션 매진 시에는 waiting_queue INSERT 까지 하므로
-- group_buy / group_buy_options / participation / waiting_queue 네 테이블이 필요하다.

CREATE TABLE group_buy (
    seq number NOT NULL PRIMARY KEY,
    product_seq number NOT NULL,
    start_at timestamp NOT NULL,
    end_at timestamp NOT NULL,
    created_at timestamp NOT NULL,
    finished_at timestamp,
    min_count number NOT NULL,
    max_count number NOT NULL,
    original_price number NOT NULL,
    final_price number NOT NULL,
    status varchar2(20) NOT NULL
);

CREATE TABLE group_buy_options (
    seq number NOT NULL PRIMARY KEY,
    group_buy_seq number NOT NULL,
    options_seq number NOT NULL,
    order_qty number NOT NULL,
    occupied_count number DEFAULT 0 NOT NULL
);

-- 상품 옵션 테이블. 공구 가격 계산이 options.additional_price를 LAZY 로딩하므로 필요.
-- Options 엔티티가 SELECT하는 컬럼(flat 옵션 컬럼 + 재고/추가금)을 모두 포함한다.
CREATE TABLE options (
    seq number NOT NULL PRIMARY KEY,
    product_seq number,
    color varchar2(100),
    options_size varchar2(100),
    volume_weight varchar2(100),
    taste varchar2(100),
    storage_type varchar2(100),
    scent_ingredient varchar2(100),
    voltage varchar2(100),
    quantity_set varchar2(100),
    size_spec varchar2(100),
    storage_capacity varchar2(100),
    memory varchar2(100),
    switch_axis varchar2(100),
    connection_type varchar2(100),
    wearable_spec varchar2(100),
    material_type varchar2(100),
    options_type varchar2(100),
    stock number NOT NULL,
    safety_stock number NOT NULL,
    additional_price number NOT NULL
);

CREATE TABLE participation (
    seq number NOT NULL PRIMARY KEY,
    group_buy_seq number NOT NULL,
    group_buy_options_seq number NOT NULL,
    member_seq number NOT NULL,
    status varchar2(20) NOT NULL,
    payment_deadline timestamp,
    promoted_at timestamp,
    created_at timestamp NOT NULL
);

-- 옵션 매진 시 대기열 등록 대상 테이블. status 컬럼 없음(대기 이탈=행 삭제).
CREATE TABLE waiting_queue (
    seq number NOT NULL PRIMARY KEY,
    group_buy_seq number NOT NULL,
    group_buy_options_seq number NOT NULL,
    member_seq number NOT NULL,
    created_at timestamp NOT NULL
);

CREATE TABLE notification (
    seq number NOT NULL PRIMARY KEY,
    type varchar2(50) NOT NULL,
    title varchar2(50) NOT NULL,
    content varchar2(100) NOT NULL,
    recipient_type varchar2(20) NOT NULL,
    recipient_seq number,
    reference_type varchar2(20) NOT NULL,
    reference_seq number,
    created_at timestamp DEFAULT sysdate NOT NULL,
    read_at timestamp
);

-- 공구와 무관하지만, 앱 시작 시 SecurityConfig.initAdminUser(CommandLineRunner)가
-- admin 테이블을 조회/INSERT 하므로(없으면 startup 예외로 컨텍스트 로드 실패) 최소 형태로 둔다.
CREATE TABLE admin (
    seq number NOT NULL PRIMARY KEY,
    id varchar2(30) NOT NULL UNIQUE,
    password varchar2(300) NOT NULL,
    adm_role number DEFAULT 0 NOT NULL,
    adm_status number DEFAULT 0 NOT NULL,
    role varchar2(250) DEFAULT 'ROLE_ADMIN' NOT NULL
);

-- participation / waiting_queue / notification 모두 save()로 INSERT 되므로 시퀀스가 필요하다 (allocationSize=1 → NOCACHE).
CREATE SEQUENCE participation_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE waiting_queue_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE notification_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE admin_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- 취소 테스트는 시나리오별로 공구/옵션을 동적 생성한다(특히 T_lock 검증은 '마감 임박' 공구가 필요).
-- 아래 더미(seq=1)와 겹치지 않도록 100부터 시작.
CREATE SEQUENCE group_buy_seq START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE group_buy_options_seq START WITH 100 INCREMENT BY 1 NOCACHE;

-- 진행 중(ONGOING) 공구 1건. end_at 은 타임존 영향 없이 항상 미래가 되도록 먼 미래 고정값.
INSERT INTO group_buy (seq, product_seq, start_at, end_at, created_at, min_count, max_count, original_price, final_price, status)
VALUES (1, 1, TIMESTAMP '2000-01-01 00:00:00', TIMESTAMP '2999-12-31 23:59:59', TIMESTAMP '2000-01-01 00:00:00', 1, 10, 10000, 8000, 'ONGOING');

-- 테스트 대상 옵션 행: 발주 가능 10자리, 점유 0에서 시작.
INSERT INTO group_buy_options (seq, group_buy_seq, options_seq, order_qty, occupied_count)
VALUES (1, 1, 1, 10, 0);

-- 기본 상품 옵션(추가금 0). 동적 생성 공구/옵션들이 options_seq=1을 참조한다.
-- 추가금 > 0 케이스가 필요한 테스트는 별도 seq로 직접 INSERT 한다.
INSERT INTO options (seq, stock, safety_stock, additional_price) VALUES (1, 0, 0, 0);
