package com.example.java.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.chat.entity.Chat;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    Optional<Chat> findByMemberSeqAndStatus(Long memberSeq, Integer status);
    
    List<Chat> findByMemberSeq(Long memberSeq);
}