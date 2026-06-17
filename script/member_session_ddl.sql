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
