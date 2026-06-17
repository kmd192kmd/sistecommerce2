package com.example.java.chat.entity;

import com.example.java.chat.enums.SenderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_LOG") 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CHAT_LOG_SEQ_GEN")
    @SequenceGenerator(name = "CHAT_LOG_SEQ_GEN", sequenceName = "CHAT_LOG_SEQ", allocationSize = 1)
    @Column(name = "SEQ")
    private Long seq;

    // 객체 매핑(Chat chat)을 하지 않고 단순 방 번호(Long)만 저장하는 구조
    @Column(name = "CHAT_SEQ", nullable = false)
    private Long chatSeq;

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