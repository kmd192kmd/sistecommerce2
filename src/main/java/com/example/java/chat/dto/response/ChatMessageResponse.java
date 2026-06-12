package com.example.java.chat.dto.response;

import com.example.java.chat.entity.ChatMessage;
import com.example.java.chat.enums.SenderType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageResponse {
    private Long seq;
    private Long chatSeq;
    private SenderType senderType;
    private Long senderId;
    private String content;
    private String createdAt;

    // 엔티티를 받아 DTO로 안전하게 변환하는 생성자
    public ChatMessageResponse(ChatMessage message) {
        this.seq = message.getSeq(); // 기존 getId()에서 getSeq()로 변경 반영!
        this.chatSeq = message.getChatSeq();
        this.senderType = message.getSenderType();
        this.senderId = message.getSenderId();
        this.content = message.getContent();
        if (message.getCreatedAt() != null) {
            this.createdAt = message.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}