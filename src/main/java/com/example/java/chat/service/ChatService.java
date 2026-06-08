package com.example.java.chat.service;

import com.example.java.chat.dto.request.ChatMessageRequest;
import com.example.java.chat.dto.response.ChatMessageResponse;
import com.example.java.chat.entity.ChatMessage;
import com.example.java.chat.entity.ChatRoom;
import com.example.java.chat.repository.ChatMessageRepository;
import com.example.java.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatRoom createRoom(Long memberId, String title) {
        ChatRoom room = ChatRoom.builder()
                .memberId(memberId)
                .title(title)
                .status("ACTIVE")
                .build();
        return chatRoomRepository.save(room);
    }

    public List<ChatRoom> getRoomsByMember(Long memberId) {
        return chatRoomRepository.findByMemberId(memberId);
    }

    @Transactional
    public ChatMessageResponse saveMessage(Long roomId, ChatMessageRequest request) {
        ChatMessage message = ChatMessage.builder()
                .roomId(roomId)
                .senderType(request.getSenderType())
                .senderId(request.getSenderId())
                .content(request.getContent())
                .build();
        
        chatMessageRepository.save(message);
        return new ChatMessageResponse(message);
    }

    public List<ChatMessageResponse> getMessageHistory(Long roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }
}