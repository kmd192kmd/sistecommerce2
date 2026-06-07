--배송테이블 지연시간 컬럼 추가
ALTER TABLE delivery ADD (delay_hours number DEFAULT 0 NOT NULL);

--상품 목록 조회 속도 개선을 위해 상품테이블에 대표이미지 컬럼을 추가
ALTER TABLE product ADD (thumbnail_url VARCHAR2(1000));

--매번 공휴일 api를 불러오지 않고 테이블에 저장하기 위해 공휴일 테이블을 생성
CREATE TABLE holiday (
        holiday_date DATE PRIMARY KEY,
        name VARCHAR2(255)
    );