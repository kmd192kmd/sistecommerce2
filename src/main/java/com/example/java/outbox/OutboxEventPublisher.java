package com.example.java.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 💡 1초마다 READY 상태인 Outbox 이벤트를 조회하여 카프카로 발행합니다.
     * 주기(fixedDelay)는 서비스 트래픽에 맞게 조절하세요. (예: 500ms ~ 5000ms)
     */
    @Scheduled(fixedDelay = 1000)
    public void publishReadyEvents() {
        // 100건씩 끊어서 가져옴 (OutboxEventRepository에 정의한 메서드)
        List<OutboxEvent> readyEvents = outboxEventRepository.findTop100ByStatusOrderBySeqAsc("READY");

        if (readyEvents.isEmpty()) {
            return;
        }

        log.info("발행 대상 Outbox 이벤트 검색됨: {}건", readyEvents.size());

        for (OutboxEvent event : readyEvents) {
            try {
                // 💡 각각의 이벤트 발행을 개별 트랜잭션으로 독립시켜 처리합니다.
                publishEventAndProfile(event);
            } catch (Exception e) {
                log.error("Outbox 이벤트 발행 중 예외 발생 - seq: {}, eventId: {}", event.getSeq(), event.getEventId(), e);
                // 실패 시 영원히 멈추는 것을 방지하기 위해 상태를 FAILED로 변경 (선택 사항)
                markAsFailed(event);
            }
        }
    }

    /**
     * 💡 REQUIRES_NEW를 통해 독립된 트랜잭션을 엽니다.
     * 카프카 전송 성공 후 즉시 커밋되어 다른 스케줄러 주기와 락이 겹치지 않게 합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishEventAndProfile(OutboxEvent event) {
        // 1. 이벤트 타입에 맞는 카프카 토픽 결정
        String topic = determineTopic(event.getEventType());

        // 2. 카프카 전송 (메시지 키는 순서 보장을 위해 aggregateId 활용)
        kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // 비동기 전송 실패 시 예외를 던져 트랜잭션 롤백 유도
                        throw new RuntimeException("카프카 메시지 전송 실패", ex);
                    }
                });

        // 3. 성공 시 상태 변경 (READY -> SUCCESS, published_at 기록)
        // 엔티티 내부에서 상태를 변경하면 JPA 더티 체킹에 의해 트랜잭션 종료 시 자동으로 업데이트됩니다.
        OutboxEvent targetEvent = outboxEventRepository.findById(event.getSeq())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));
        targetEvent.changeStatusToSuccess();
        
        log.info("카프카 발행 성공 및 Outbox 업데이트 완료 - seq: {}, topic: {}", event.getSeq(), topic);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(OutboxEvent event) {
        outboxEventRepository.findById(event.getSeq()).ifPresent(target -> {
            target.changeStatusToFailed();
            outboxEventRepository.save(target);
        });
    }

    /**
     * 💡 이벤트 타입에 따라 발행할 카프카 토픽명을 매핑하는 헬퍼 메서드
     */
    private String determineTopic(String eventType) {
        switch (eventType) {
            case "OrderPaidEvent":
                return "order-paid-topic";
            // 추후 다른 이벤트(예: OrderCanceledEvent)가 추가되면 이곳에 확장
            default:
                return "default-outbox-topic";
        }
    }
    
 // 새벽 3시에 일주일 지난 성공 데이터 일괄 삭제 스케줄러 예시
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanUpOldOutboxEvents() {
        LocalDateTime deleteTargetTime = LocalDateTime.now().minusDays(7);
        outboxEventRepository.deleteByStatusAndCreatedAtBefore("SUCCESS", deleteTargetTime);
        log.info("일주일 지난 Outbox 성공 데이터 청소 완료");
    }
    
}