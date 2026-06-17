package com.example.java.chat.repository;

import com.example.java.chat.entity.Chat;
import com.example.java.chat.enums.ChatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    // 특정 회원의 모든 채팅 내역 조회 (최신순)
    List<Chat> findByMemberSeqOrderByCreatedAtDesc(Long memberSeq);

    // 특정 회원의 현재 진행 중인(0: 활성화) 채팅 단건 조회
    Optional<Chat> findByMemberSeqAndStatus(Long memberSeq, ChatStatus status);
    
    List<Chat> findByStatusOrderByCreatedAtDesc(int status);
}