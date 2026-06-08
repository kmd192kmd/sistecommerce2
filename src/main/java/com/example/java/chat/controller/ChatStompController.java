package com.example.java.chat.controller;

import com.example.java.chat.dto.request.ChatMessageRequest;
import com.example.java.chat.dto.response.ChatMessageResponse;
import com.example.java.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage/{roomId}")
    public void handleMessage(@DestinationVariable("roomId") Long roomId, @Payload ChatMessageRequest request) {
        
        // 1. 메시지 DB 저장
        ChatMessageResponse responseDto = chatService.saveMessage(roomId, request);

        // 2. 해당 방 구독자들에게 브로드캐스팅
        messagingTemplate.convertAndSend("/topic/room/" + roomId, responseDto);
    }
}