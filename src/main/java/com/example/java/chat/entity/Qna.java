package com.example.java.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qna")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Qna {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(nullable = false)
    private Long chatSeq;

    private Long adminSeq;

    @Column(columnDefinition = "TEXT")
    private String lastAiResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QnaStatus status; // AI_ANSWERED, PENDING_ADMIN, ADMIN_COMPLETED, RE_QUESTIONED_TO_AI

    private LocalDateTime updatedAt;
}