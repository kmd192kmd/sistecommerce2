package com.example.java.member.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.java.member.entity.Member;
import com.example.java.member.service.PasswordResetService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /** ① 아이디 + 이메일 입력 폼 */
    @GetMapping("/find-password")
    public String findPasswordPage() {
        return "member/find-password";
    }

    /** ② 메일 발송 */
    @PostMapping("/find-password")
    public String sendResetEmail(
            @RequestParam String username,
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        try {
            passwordResetService.sendResetEmail(username, email);
        } catch (Exception e) {
            log.error("비밀번호 재설정 메일 발송 오류", e);
        }

        // 회원 존재 여부 노출 방지 — 항상 동일 메시지
        redirectAttributes.addFlashAttribute("sent", true);
        return "redirect:/member/find-password";
    }

    /** ③ 새 비밀번호 입력 폼 (토큰 검증) */
    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam String token,
            Model model) {

        try {
            Member member = passwordResetService.validateToken(token);
            model.addAttribute("token", token);
            model.addAttribute("username", member.getUsername());
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
        }

        return "member/reset-password";
    }

    /** ④ 비밀번호 변경 */
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes) {

        try {
            passwordResetService.resetPassword(token, newPassword);
            redirectAttributes.addFlashAttribute("resetSuccess", true);
            return "redirect:/member/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/member/reset-password?token=" + token;
        } catch (Exception e) {
            log.error("비밀번호 변경 오류", e);
            redirectAttributes.addFlashAttribute("errorMsg", "비밀번호 변경 중 오류가 발생했습니다.");
            return "redirect:/member/reset-password?token=" + token;
        }
    }
}
