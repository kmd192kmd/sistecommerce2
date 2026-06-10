package com.example.java.member.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_token")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_token_seq")
    @SequenceGenerator(name = "email_token_seq", sequenceName = "email_token_seq", allocationSize = 1)
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @Column(name = "token_hash", nullable = false, unique = true, length = 200)
    private String tokenHash;

    /**
     * EMAIL_VERIFY(30분) / PW_RESET(1시간) / ACCOUNT_UNLOCK(1시간)
     */
    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "used_yn", nullable = false, length = 1)
    @Builder.Default
    private String usedYn = "N";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expireAt);
    }

    public boolean isUsed() {
        return "Y".equals(this.usedYn);
    }

    public void markUsed() {
        this.usedYn = "Y";
    }
}
