package com.example.java.member.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.member.service.EmailVerificationService;
import com.example.java.member.service.EmailVerificationService.SendResult;
import com.example.java.member.service.EmailVerificationService.VerifyResult;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원가입 이메일 인증 API.
 */
@Slf4j
@RestController
@RequestMapping("/api/member/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    /** 인증코드 발송(중복확인 포함) */
    @PostMapping("/send-code")
    public Map<String, Object> sendCode(@RequestParam(name = "email") String email,
                                        HttpSession session) {
        try {
            SendResult result = emailVerificationService.sendCode(email, session);
            return switch (result) {
                case SENT      -> Map.of("success", true, "message", "인증번호를 발송했습니다. (5분 내 입력)");
                case DUPLICATE -> Map.of("success", false, "reason", "DUPLICATE", "message", "이미 가입된 이메일입니다.");
                case INVALID   -> Map.of("success", false, "reason", "INVALID", "message", "올바른 이메일을 입력해 주세요.");
            };
        } catch (Exception e) {
            log.error("인증코드 발송 오류 - email: {}", email, e);
            return Map.of("success", false, "reason", "SEND_FAIL", "message", "인증 메일 발송에 실패했습니다.");
        }
    }

    /** 인증코드 확인 */
    @PostMapping("/verify-code")
    public Map<String, Object> verifyCode(@RequestParam(name = "email") String email,
                                          @RequestParam(name = "code") String code,
                                          HttpSession session) {
        VerifyResult result = emailVerificationService.verifyCode(email, code, session);
        String message = switch (result) {
            case OK       -> "이메일 인증이 완료되었습니다.";
            case NO_CODE  -> "먼저 인증메일을 요청해 주세요.";
            case MISMATCH -> "인증을 요청한 이메일과 다릅니다.";
            case EXPIRED  -> "인증번호가 만료되었습니다. 다시 요청해 주세요.";
            case WRONG    -> "인증번호가 일치하지 않습니다.";
        };
        return Map.of("verified", result == VerifyResult.OK, "message", message);
    }
}
