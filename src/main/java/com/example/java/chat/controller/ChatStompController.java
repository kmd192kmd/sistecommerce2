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
import com.example.java.member.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage/{roomId}")
    @SendTo("/topic/rooms/{roomId}")
    public ChatMessageResponse sendMessage(
            @DestinationVariable("roomId") Long roomId, 
            @Payload ChatMessageRequest request, 
            Principal principal) {
        
        // 1. SecurityContext(Principal)에서 로그인한 사용자의 고유 번호 추출
        Long memberSeq = getMemberSeqFromPrincipal(principal);

        // 2. ChatService가 요구하는 4개의 파라미터로 풀어서 전달합니다.
        // 에러가 났던 부분 해결!
        return chatService.saveMessage(
            roomId, 
            memberSeq, 
            request.getContent(), 
            request.getSenderType()
        );
    }

    /**
     * Principal 객체로부터 안전하게 회원의 고유 번호(memberSeq)를 반환하는 유틸리티 메서드
     */
    private Long getMemberSeqFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("인증 정보가 존재하지 않습니다. 로그인이 필요합니다.");
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) ((Authentication) principal).getPrincipal();
        return userDetails.getMemberSeq();
    }
}