package com.example.java.outbox;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    // 💡 DB의 JSON 타입은 String으로 매핑하여 JSON 텍스트를 그대로 저장합니다.
    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // 'READY', 'SUCCESS', 'FAILED' 등

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // 💡 나중에 스케줄러가 카프카 발행 성공 후 상태를 업데이트할 때 사용할 메서드
    public void changeStatusToSuccess() {
        this.status = "SUCCESS";
        this.publishedAt = LocalDateTime.now();
    }

    public void changeStatusToFailed() {
        this.status = "FAILED";
    }
}