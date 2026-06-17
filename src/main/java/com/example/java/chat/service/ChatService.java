package com.example.java.chat.service;

import com.example.java.chat.dto.response.ChatMessageResponse;
import com.example.java.chat.entity.ChatMessage;
import com.example.java.chat.enums.SenderType;
import com.example.java.chat.entity.Chat;
import com.example.java.chat.repository.ChatMessageRepository;
import com.example.java.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 새로운 채팅방(Chat) 생성
     */
    @Transactional
    public Chat createRoom(Long memberSeq, String title, String category) {
        Chat room = Chat.builder()
                .memberSeq(memberSeq)
                .title(title)
                .category(category)
                .status(0) // 0: 활성화 상태
                .build();
        return chatRepository.save(room);
    }
    
    /**
     * 특정 회원의 전체 채팅 내역 조회
     */
    public List<Chat> getRoomsByMember(Long memberId) {
        // ChatRepository에 findByMemberSeq 메서드가 정의되어 있어야 합니다.
        // 예: List<Chat> findByMemberSeq(Long memberSeq);
    	return chatRepository.findByMemberSeqOrderByCreatedAtDesc(memberId);
    }

    /**
     * [메시지 저장 로직]
     * AI의 경우 senderId 파라미터에 null을 넘기면 DB에도 null로 정상 저장됩니다.
     */
    @Transactional
    public ChatMessageResponse saveMessage(Long roomId, Long senderId, String content, SenderType senderType) {
        
        // 엔티티 매핑 없이 넘어온 방 번호(Long)를 그대로 저장합니다.
        ChatMessage message = ChatMessage.builder()
                .chatSeq(roomId)        // 컨트롤러에서 넘어온 방 번호
                .senderType(senderType) // USER, AI, ADMIN
                .senderId(senderId)     // 회원/관리자의 PK (AI가 보낼 땐 null)
                .content(content)
                .build();
        
        chatMessageRepository.save(message);
        
        // 저장된 엔티티를 프론트엔드 반환용 DTO로 변환
        return new ChatMessageResponse(message);
    }

    /**
     * [메시지 조회 로직]
     * 특정 채팅방(chatSeq)에 해당하는 모든 메시지를 과거순으로 가져옵니다.
     */
    public List<ChatMessageResponse> getMessageHistory(Long roomId) {
        return chatMessageRepository.findByChatSeqOrderByCreatedAtAsc(roomId).stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }
    public List<Chat> getAllActiveRooms() {
        // 예: 상태값이 0(활성화)인 모든 채팅방을 생성일 역순으로 조회
        return chatRepository.findByStatusOrderByCreatedAtDesc(0); 
    }
    public boolean hasAdminJoined(Long roomId) {
        // ADMIN(관리자)이 해당 방에 단 한 번이라도 메시지를 남겼다면 true 반환
        return chatMessageRepository.existsByChatSeqAndSenderType(roomId, SenderType.ADMIN);
    }
}