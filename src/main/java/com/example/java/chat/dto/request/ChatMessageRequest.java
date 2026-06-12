package com.example.java.chat.dto.request;

import com.example.java.chat.enums.SenderType;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChatMessageRequest {
    private SenderType senderType;
    private Long senderId;
    private String content;
}