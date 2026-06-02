package com.example.java.member.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_preferences")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_preferences_seq")
    @SequenceGenerator(name = "notification_preferences_seq", sequenceName = "notification_preferences_seq", allocationSize = 1)
    private Long seq;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @Column(name = "email_yn", length = 1, nullable = false)
    @Builder.Default
    private String emailYn = "N";

    @Column(name = "sms_yn", length = 1, nullable = false)
    @Builder.Default
    private String smsYn = "N";

    @Column(name = "push_yn", length = 1, nullable = false)
    @Builder.Default
    private String pushYn = "N";

    @Column(name = "marketing_email_yn", length = 1, nullable = false)
    @Builder.Default
    private String marketingEmailYn = "N";

    @Column(name = "marketing_sms_yn", length = 1, nullable = false)
    @Builder.Default
    private String marketingSmsYn = "N";

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
