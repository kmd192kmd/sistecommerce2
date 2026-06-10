package com.example.java.chat.repository;

import com.example.java.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // 이전: findByRoomIdOrderByCreatedAtAsc
    // 변경: roomId 대신 chatSeq 컬럼을 기준으로 오름차순 조회
    List<ChatMessage> findByChatSeqOrderByCreatedAtAsc(Long chatSeq);
}