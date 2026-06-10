package com.example.java.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Chat {

    @Id 
    // 오라클 시퀀스를 사용하도록 전략 변경
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CHAT_SEQ_GEN")
    @SequenceGenerator(name = "CHAT_SEQ_GEN", sequenceName = "CHAT_SEQ", allocationSize = 1)
    @Column(name = "SEQ")
    private Long seq;

    @Column(name = "MEMBER_SEQ", nullable = false)
    private Long memberSeq;

    @Column(name = "ADMIN_SEQ")
    @Builder.Default
    private Long adminSeq = 1L;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "CATEGORY")
    private String category;

    @Column(name = "STATUS", nullable = false)
    @Builder.Default
    private Integer status = 0;

    @Column(name = "CREATED_AT")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
