package com.example.java.chat.dto.response;

import com.example.java.chat.entity.ChatMessage;
import com.example.java.chat.entity.SenderType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {
    private Long id;
    private Long roomId;
    private SenderType senderType;
    private Long senderId;
    private String content;
    private LocalDateTime createdAt;

    public ChatMessageResponse(ChatMessage message) {
        this.id = message.getId();
        this.roomId = message.getRoomId();
        this.senderType = message.getSenderType();
        this.senderId = message.getSenderId();
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
    }
}