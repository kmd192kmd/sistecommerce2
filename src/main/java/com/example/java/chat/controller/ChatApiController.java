package com.example.java.chat.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.java.chat.dto.request.ChatMessageRequest;
import com.example.java.chat.dto.response.ChatMessageResponse;
import com.example.java.chat.entity.Chat;
import com.example.java.chat.service.ChatService;
import com.example.java.member.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatService chatService;

    /**
     * [HTTP GET] 로그인한 사용자의 채팅방 목록을 조회합니다.
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<Chat>> getMyRooms(Principal principal) {
        Long memberSeq = getMemberSeqFromPrincipal(principal);
        return ResponseEntity.ok(chatService.getRoomsByMember(memberSeq));
    }

    /**
     * [HTTP POST] 새로운 채팅방을 생성합니다.
     */
    @PostMapping("/rooms")
    public ResponseEntity<Chat> createRoom(
            Principal principal,
            @RequestParam("title") String title,
            @RequestParam("category") String category) {
        
        Long memberSeq = getMemberSeqFromPrincipal(principal);
        Chat room = chatService.createRoom(memberSeq, title, category);
        return ResponseEntity.ok(room);
    }

    /**
     * [HTTP GET] 특정 채팅방의 메시지 내역을 조회합니다.
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getHistory(@PathVariable("roomId") String roomIdStr) {
        if ("undefined".equals(roomIdStr)) {
            return ResponseEntity.badRequest().build();
        }
        Long roomId = Long.parseLong(roomIdStr);
        return ResponseEntity.ok(chatService.getMessageHistory(roomId));
    }


    /**
     * SecurityContext의 Principal 객체로부터 안전하게 회원의 고유 번호(memberSeq)를 반환하는 유틸리티 메서드
     */
    private Long getMemberSeqFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("인증 정보가 존재하지 않습니다. 로그인이 필요합니다.");
        }
        
        // Principal 객체를 Authentication으로 캐스팅한 후 커스텀 UserDetails를 꺼냅니다.
        CustomUserDetails userDetails = (CustomUserDetails) ((Authentication) principal).getPrincipal();
        return userDetails.getMemberSeq();
    }
}