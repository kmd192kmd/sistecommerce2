package com.example.java.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.chat.dto.response.ChatMessageResponse;
import com.example.java.chat.entity.ChatRoom;
import com.example.java.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatService chatService; // 이미 주입받고 있습니다!

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getMyRooms(@RequestParam("memberId") Long memberId) {
        return ResponseEntity.ok(chatService.getRoomsByMember(memberId));
    }

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoom> createRoom(
            @RequestParam Long memberSeq, // memberId -> memberSeq
            @RequestParam String title) {
        
        ChatRoom room = chatService.createRoom(memberSeq, title);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getHistory(@PathVariable("roomId") Long roomId) {
        return ResponseEntity.ok(chatService.getMessageHistory(roomId));
    }
}