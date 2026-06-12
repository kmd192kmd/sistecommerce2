package com.example.java.chat.service;

import com.example.java.chat.dto.response.ChatMessageResponse;
import com.example.java.chat.enums.SenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatbotService {

    private final ChatService chatService;
    // 특정 구독자(채팅방)에게 메시지를 쏴주는 스프링 내장 객체
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 사용자의 메시지를 받아 백그라운드에서 AI API를 호출하고 답변을 전송합니다.
     */
    @Async // 이 메서드는 기존 흐름을 방해하지 않고 별도의 스레드에서 백그라운드로 실행됩니다.
    public void processUserMessageAndRespond(Long roomId, String userMessage) {
        try {
            log.info("[방 번호: {}] AI가 답변을 생성 중입니다... (사용자 메시지: {})", roomId, userMessage);
            
            // 1. AI API 호출 대기 시간 시뮬레이션 (1.5초 대기)
            // 실제 연동 시에는 이 부분을 진짜 API 호출 로직으로 바꿉니다.
            Thread.sleep(1500); 

            // 2. AI 응답 생성
            String aiResponse = generateAiResponse(userMessage);

            // 3. AI의 답변을 DB에 저장 (AI는 특정 회원이 아니므로 senderId는 null로 처리)
            ChatMessageResponse savedAiMessage = chatService.saveMessage(
                    roomId,
                    null, 
                    aiResponse,
                    SenderType.AI
            );

            // 4. DB 저장이 완료되면 해당 채팅방(/topic/chat/{roomId})에 들어와 있는 사람의 화면에 AI 메시지를 쏴줍니다.
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, savedAiMessage);
            log.info("[방 번호: {}] AI 답변 전송 완료", roomId);

        } catch (Exception e) {
            log.error("AI 응답 처리 중 오류 발생", e);
        }
    }

    /**
     * 임시 AI 응답 생성기 (TODO: 차후 실제 LLM API 연동)
     */
    private String generateAiResponse(String message) {
        if (message.contains("배송")) {
            return "배송은 영업일 기준 2~3일 정도 소요됩니다. 🚚 추가로 궁금한 점이 있으신가요?";
        } else if (message.contains("환불") || message.contains("취소")) {
            return "취소 및 환불은 '마이페이지 > 주문내역'에서 신청하실 수 있습니다. 💳";
        } else {
            return "안녕하세요! 쇼핑몰 AI 챗봇입니다. 봇이 처리할 수 없는 복잡한 문의라면 메뉴에서 '상담원 연결'을 선택해 주세요. 🤖";
        }
    }
}