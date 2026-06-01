package com.example.java.member.controller;

import com.example.java.member.dto.MemberDto;
import com.example.java.member.service.MemberLoginService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberLoginService memberLoginService;

    @GetMapping("/login")
    public String loginPage() {
        return "member/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        boolean success = memberLoginService.login(username, password, session);

        if (!success) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "member/login";
        }

        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        memberLoginService.logout(session);
        return "redirect:/members/login";
    }
}