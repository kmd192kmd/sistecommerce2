package com.example.java.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/admin")
public class AdminChatController {

    /**
     * 실시간 1:1 고객 상담 관리 페이지 호출
     * * [접근 권한 설정]
     * - 기본적으로 스프링 시큐리티의 'ROLE_ADMIN' 권한이 필요합니다.
     * - 추가로 principal 객체에 담긴 admRole 값이 1(최고관리자) 또는 2(CS관리자)인 경우만 접근을 허용합니다.
     * * @return templates/admin/chat/chat-admin.html
     */
    @PreAuthorize("hasRole('ROLE_ADMIN') and (authentication.principal.admRole == 1 or authentication.principal.admRole == 2)")
    @GetMapping("/chat")
    public String chatAdminPage() {
        log.info("▶ [AdminChatController] 관리자 실시간 채팅 상담 페이지(chat-admin) 진입");
        
        //prefix: src/main/resources/templates/
        //suffix: .html
        return "admin/chat/chat-admin";
    }
}