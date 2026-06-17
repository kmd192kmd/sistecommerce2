package com.example.java.common.notification.dto;

import java.time.LocalDateTime;

import com.example.java.common.notification.entity.Notification;

import lombok.Builder;
import lombok.Getter;

/**
 * 알림 단건 조회 응답 (회원 노출용).
 * recipientType/recipientSeq 같은 내부 식별 정보는 내려보내지 않는다.
 */
@Getter
@Builder
public class NotificationResponse {

    private final Long seq;
    private final String type;
    private final String title;
    private final String content;
    private final String referenceType;
    private final Long referenceSeq;
    private final LocalDateTime createdAt;
    private final LocalDateTime readAt;
    private final boolean read;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .seq(n.getSeq())
                .type(n.getType().name())
                .title(n.getTitle())
                .content(n.getContent())
                .referenceType(n.getReferenceType().name())
                .referenceSeq(n.getReferenceSeq())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .read(n.getReadAt() != null)
                .build();
    }
}
