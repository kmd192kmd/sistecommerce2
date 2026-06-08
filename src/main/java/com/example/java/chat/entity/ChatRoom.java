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

    // 필드명 변경: memberId -> memberSeq
    @Column(name = "member_seq", nullable = false)
    private Long memberSeq;

    @Column(name = "admin_seq")
    @Builder.Default
    private Long adminSeq = 1L; // 1번 관리자 고정

    @Column(nullable = false)
    private String title;

    // String "ACTIVE" 대신 Integer 0 사용
    @Column(nullable = false)
    @Builder.Default
    private Integer status = 0; // 0: 활성(ACTIVE), 1: 종료(CLOSED)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}