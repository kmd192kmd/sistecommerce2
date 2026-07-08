package com.example.java.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    // 💡 카프카 발행용 스케줄러가 'READY' 상태인 이벤트를 오래된 순으로 가져올 때 사용
    List<OutboxEvent> findTop100ByStatusOrderBySeqAsc(String status);

	void deleteByStatusAndCreatedAtBefore(String string, LocalDateTime deleteTargetTime);
}