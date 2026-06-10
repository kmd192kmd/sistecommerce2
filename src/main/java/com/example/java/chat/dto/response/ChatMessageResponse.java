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
        this.id = message.getSeq();               // PK 매핑 (기존 getId -> getSeq)
        this.roomId = message.getChatSeq();       // 방 번호 매핑 (기존 getRoomId -> getChatSeq)
        this.senderType = message.getSenderType();
        this.senderId = message.getSenderId();    // 발신자 번호 (AI면 null이 들어감)
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
    }
}