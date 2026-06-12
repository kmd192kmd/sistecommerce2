package com.example.java.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.example.java.chat.dto.request.ChatMessageRequest;
import com.example.java.chat.dto.response.ChatMessageResponse;
import com.example.java.chat.service.ChatService;
import com.example.java.chat.service.AiChatbotService; // 👈 새로 추가
import com.example.java.member.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final AiChatbotService aiChatbotService; // 👈 새로 추가

    @MessageMapping("/chat/{roomId}/send")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessageResponse sendMessage(
            @DestinationVariable("roomId") Long roomId, 
            @Payload ChatMessageRequest request, 
            Principal principal) {
        
        Long memberSeq = getMemberSeqFromPrincipal(principal);
        log.info("채팅 수신 - 방: {}, 발신자: {}, 내용: {}", roomId, memberSeq, request.getContent());

        // 1. 사용자 메시지를 DB에 저장 (이 결과는 @SendTo를 통해 즉시 사용자의 화면에 나타남)
        ChatMessageResponse userMessage = chatService.saveMessage(
            roomId, 
            memberSeq, 
            request.getContent(), 
            request.getSenderType()
        );

        // 2. 비동기 AI 답변 생성 지시! 
        // (사용자 화면에는 이미 위 메시지가 떴고, 백그라운드에서 AI가 생각하기 시작합니다)
        // 챗봇용 방인지 체크하는 로직을 나중에 추가할 수 있습니다.
        aiChatbotService.processUserMessageAndRespond(roomId, request.getContent());

        return userMessage;
    }

    private Long getMemberSeqFromPrincipal(Principal principal) {
        if (principal == null) {
            log.error("웹소켓 인증 정보 없음");
            throw new IllegalArgumentException("인증 정보가 존재하지 않습니다. 로그인이 필요합니다.");
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) ((Authentication) principal).getPrincipal();
        return userDetails.getMemberSeq();
    }
}