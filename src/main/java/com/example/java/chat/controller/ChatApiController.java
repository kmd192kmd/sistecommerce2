package com.example.java.chat.controller;

import com.example.java.chat.dto.response.ChatMessageResponse;
import com.example.java.chat.entity.ChatRoom;
import com.example.java.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatService chatService;

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getMyRooms(@RequestParam("memberId") Long memberId) {
        return ResponseEntity.ok(chatService.getRoomsByMember(memberId));
    }

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoom> createRoom(@RequestParam("memberId") Long memberId, @RequestParam("title") String title) {
        return ResponseEntity.ok(chatService.createRoom(memberId, title));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getHistory(@PathVariable("roomId") Long roomId) {
        return ResponseEntity.ok(chatService.getMessageHistory(roomId));
    }
}