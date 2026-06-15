package com.example.java.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate; // 👈 추가된 임포트
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.example.java.chat.dto.request.ChatMessageRequest;
import com.example.java.chat.dto.response.ChatMessageResponse;
import com.example.java.chat.enums.SenderType;
import com.example.java.chat.service.AiChatbotService;
import com.example.java.chat.service.ChatService;
import com.example.java.member.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final AiChatbotService aiChatbotService;
    
    // 👈 웹소켓으로 메시지를 직접 쏴줄 수 있는 메신저 템플릿 추가
    private final SimpMessagingTemplate messagingTemplate; 

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

        // 2. OpenAI API 호출 및 답변 받아오기 (리턴값을 변수에 저장)
        String aiAnswerText = aiChatbotService.processUserMessageAndRespond(roomId, memberSeq, request.getContent());

        // 3. AI의 답변을 DB에 저장하고 응답 객체(DTO) 생성
        // (발신자 ID는 AI를 뜻하는 0L이나 1L 혹은 시스템 룰에 맞게 입력, senderType은 "AI"로 고정)
        ChatMessageResponse aiMessage = chatService.saveMessage(
            roomId, 
            0L, // 시스템/AI의 가상 ID (프로젝트 DB 설정에 따라 null이 안되면 0 등을 사용)
            aiAnswerText, 
            SenderType.AI
        );

        // 4. 🚨 [핵심] 완성된 AI 메시지를 해당 채팅방을 구독 중인 프론트엔드로 직접 발송!
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, aiMessage);

        // 5. 사용자가 보낸 메시지 반환 (@SendTo 작동)
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