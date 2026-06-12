package com.example.java.chat.repository;

import com.example.java.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 필드명(chatSeq)과 정렬 조건(CreatedAtAsc)을 정확히 조합한 쿼리 메서드
    List<ChatMessage> findByChatSeqOrderByCreatedAtAsc(Long chatSeq);
}