package com.example.java.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_LOG") 
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CHAT_LOG_SEQ_GEN")
    @SequenceGenerator(name = "CHAT_LOG_SEQ_GEN", sequenceName = "CHAT_LOG_SEQ", allocationSize = 1)
    @Column(name = "SEQ")
    private Long seq; // 기존 id에서 seq로 변경됨 -> 여기서 getSeq()가 생성됨

    @Column(name = "CHAT_SEQ", nullable = false)
    private Long chatSeq; // 기존 roomId에서 chatSeq로 변경됨 -> 여기서 getChatSeq()가 생성됨

    @Enumerated(EnumType.STRING)
    @Column(name = "SENDER_TYPE", nullable = false)
    private SenderType senderType;

    @Column(name = "SENDER_ID", nullable = true) 
    private Long senderId; 

    @Column(name = "CONTENT")
    private String content;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
}