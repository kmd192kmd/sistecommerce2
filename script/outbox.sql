--kafka outbox pattern을 위한 테이블 스키마
CREATE TABLE outbox_event (
    seq BIGINT PRIMARY KEY AUTO_INCREMENT,

    event_id VARCHAR(36) NOT NULL UNIQUE,

    event_type VARCHAR(100) NOT NULL,

    aggregate_type VARCHAR(50) NOT NULL,

    aggregate_id BIGINT NOT NULL,

    payload JSON NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'READY',

    created_at DATETIME NOT NULL,

    published_at DATETIME NULL
);