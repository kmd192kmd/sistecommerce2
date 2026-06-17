package com.example.java.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.example.java.chat.dto.request.ChatMessageRequest;
import com.example.java.chat.dto.response.ChatMessageResponse;
// 본인의 프로젝트 패키지 경로에 맞게 Enum을 정확히 임포트해주세요
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
    private final SimpMessagingTemplate messagingTemplate; 

    /**
     * 웹소켓을 통해 들어오는 실시간 메시지를 처리하는 엔드포인트
     */
    @MessageMapping("/chat/{roomId}/send")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessageResponse sendMessage(
            @DestinationVariable("roomId") Long roomId, 
            @Payload ChatMessageRequest request, 
            Principal principal) {
        
        // 1. 발신자의 고유 번호(Seq) 가져오기
        Long memberSeq = getMemberSeqFromPrincipal(principal);
        log.info("▶ [STOMP 수신] 방: {}, 발신자: {}, 구분: {}, 내용: {}", 
                roomId, memberSeq, request.getSenderType(), request.getContent());

        // 2. 누가 보냈든(USER, ADMIN) 일단 들어온 메시지를 DB에 저장
        ChatMessageResponse savedMessage = chatService.saveMessage(
            roomId, 
            memberSeq, 
            request.getContent(), 
            // 프론트에서 문자열로 넘어온 SenderType을 Enum으로 변환하여 저장
            SenderType.valueOf(request.getSenderType().toString()) 
        );

        // 3. 발신자가 '고객(USER)'일 때만 AI 개입 여부를 판단
        if (SenderType.USER.toString().equals(request.getSenderType().toString())) {
            
            // 🌟 [핵심 로직] 이 방에 관리자(ADMIN)가 이미 개입하여 답변을 남겼는지 확인
            boolean isAdminJoined = chatService.hasAdminJoined(roomId);

            if (!isAdminJoined) {
                // 관리자가 아직 개입하지 않은 방이므로 AI가 답변 생성
                log.info("▶ [AI 봇] 관리자 미배정 상태 확인. AI 챗봇이 답변을 생성합니다.");
                
                String aiAnswerText = aiChatbotService.processUserMessageAndRespond(
                        roomId, memberSeq, request.getContent()
                );

                // AI의 답변을 DB에 저장 (발신자 ID는 0L, 타입은 AI)
                ChatMessageResponse aiMessage = chatService.saveMessage(
                    roomId, 
                    0L, 
                    aiAnswerText, 
                    SenderType.AI
                );

                // AI의 답변을 해당 채팅방 구독자(프론트엔드)들에게 직접 쏴줌
                messagingTemplate.convertAndSend("/topic/chat/" + roomId, aiMessage);
                
            } else {
                // 관리자가 이미 답변을 남긴 이력이 있다면, AI는 더 이상 개입하지 않음
                log.info("▶ [AI 봇] 해당 방({})은 이미 상담사(ADMIN)가 배정되어 대화 중이므로 AI는 침묵합니다.", roomId);
            }
            
        } else {
            // 발신자가 관리자(ADMIN)인 경우
            log.info("▶ [시스템] 발신자가 상담사(ADMIN)이므로 AI 답변 로직을 생략합니다.");
        }

        // 4. 최초에 저장했던 메시지(고객 또는 관리자가 쓴 원본 메시지) 반환 
        // -> @SendTo 어노테이션에 의해 화면에 렌더링 됨
        return savedMessage;
    }

    /**
     * SecurityContext의 Principal 객체로부터 회원의 고유 번호(memberSeq) 추출
     */
    private Long getMemberSeqFromPrincipal(Principal principal) {
        if (principal == null) {
            log.error("웹소켓 인증 정보 없음: Principal is null");
            throw new IllegalArgumentException("인증 정보가 존재하지 않습니다. 로그인이 필요합니다.");
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) ((Authentication) principal).getPrincipal();
        return userDetails.getMemberSeq();
    }
}