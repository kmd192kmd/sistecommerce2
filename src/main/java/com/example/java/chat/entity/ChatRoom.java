package com.example.java.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId; // 상담을 요청한 사용자(고객) ID

    @Column(nullable = false)
    private String title; // 채팅방 제목 (예: "배송 관련 문의")

    @Column(nullable = false)
    private String status; // ACTIVE, CLOSED 등

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}