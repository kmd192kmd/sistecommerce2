package com.example.java.storefront;

import com.example.java.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면(헤더 fragment)에서 쓰는 공통 모델 속성 주입.
 * - auth: 세션의 로그인 사용자(없으면 null) → 헤더 로그인/로그아웃 분기에 사용
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("auth")
    public MemberDto auth(HttpSession session) {
        return (MemberDto) session.getAttribute("auth");
    }
}
