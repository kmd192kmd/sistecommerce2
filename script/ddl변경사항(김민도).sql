--배송테이블 지연시간 컬럼 추가
ALTER TABLE delivery ADD (delay_hours number DEFAULT 0 NOT NULL);