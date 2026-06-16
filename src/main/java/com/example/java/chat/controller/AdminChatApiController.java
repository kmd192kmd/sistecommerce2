package com.example.java.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.chat.dto.response.ChatMessageResponse;
import com.example.java.chat.entity.Chat;
import com.example.java.chat.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
public class AdminChatApiController {

    private final ChatService chatService;

    /**
     * [HTTP GET] 관리자용: 현재 활성화된(상담 대기/진행 중인) 모든 채팅방 목록 조회
     * (최고관리자와 CS관리자만 접근 가능)
     */
    @PreAuthorize("hasRole('ROLE_ADMIN') and (authentication.principal.admRole == 1 or authentication.principal.admRole == 2)")
    @GetMapping("/rooms")
    public ResponseEntity<List<Chat>> getActiveRooms() {
        log.info("▶ [Admin API] 활성화된 채팅방 목록 조회 요청");
        // TODO: ChatService에 상태가 '0(활성화)'인 모든 방을 최신순으로 가져오는 메서드를 추가해야 합니다.
        List<Chat> activeRooms = chatService.getAllActiveRooms(); 
        return ResponseEntity.ok(activeRooms);
    }

    /**
     * [HTTP GET] 관리자용: 특정 채팅방의 과거 대화 내역 조회
     */
    @PreAuthorize("hasRole('ROLE_ADMIN') and (authentication.principal.admRole == 1 or authentication.principal.admRole == 2)")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(@PathVariable("roomId") String roomIdStr) {
        if ("undefined".equals(roomIdStr)) {
            return ResponseEntity.badRequest().build();
        }
        Long roomId = Long.parseLong(roomIdStr);
        log.info("▶ [Admin API] 채팅방 [{}] 대화 내역 조회 요청", roomId);
        
        // 기존 ChatApiController에서 사용하신 메서드를 그대로 재사용!
        return ResponseEntity.ok(chatService.getMessageHistory(roomId));
    }
}