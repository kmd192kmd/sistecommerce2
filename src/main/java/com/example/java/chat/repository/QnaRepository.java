package com.example.java.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.chat.entity.Qna;

public interface QnaRepository extends JpaRepository<Qna, Long> {
    Qna findByChatSeq(Long chatSeq);
}