package com.example.java.chat.service;

import com.example.java.chat.dto.response.ChatMessageResponse;
import com.example.java.chat.entity.ChatMessage;
import com.example.java.chat.entity.Chat; 
import com.example.java.chat.entity.SenderType;
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

    @Transactional
    public Chat createRoom(Long memberSeq, String title, String category) {
        Chat room = Chat.builder()
                .memberSeq(memberSeq)
                .title(title)
                .category(category)
                .status(0) 
                .build();
        return chatRepository.save(room);
    }
    
    public List<Chat> getRoomsByMember(Long memberId) {
        return chatRepository.findByMemberSeq(memberId);
    }

    /**
     * [메시지 저장 로직]
     * AI의 경우 senderId 파라미터에 null을 넘기면 DB에도 null로 정상 저장됩니다.
     */
    @Transactional
    public ChatMessageResponse saveMessage(Long roomId, Long senderId, String content, SenderType senderType) {
        
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
     * 변경된 Repository 메서드(findByChatSeqOrderByCreatedAtAsc)를 호출합니다.
     */
    public List<ChatMessageResponse> getMessageHistory(Long roomId) {
        return chatMessageRepository.findByChatSeqOrderByCreatedAtAsc(roomId).stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }
}