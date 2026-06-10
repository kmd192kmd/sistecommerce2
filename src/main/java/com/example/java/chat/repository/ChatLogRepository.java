package com.example.java.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.chat.entity.ChatLog;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {}