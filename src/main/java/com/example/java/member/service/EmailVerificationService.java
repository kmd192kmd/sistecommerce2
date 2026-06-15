package com.example.java.member.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * 회원가입 이메일 인증(코드 방식) 서비스.
 *
 * 가입 전 단계라 회원이 없어 email_token(member_seq NOT NULL)을 못 쓴다.
 * → 인증 상태를 세션에 보관(이 프로젝트 세션은 Redis 백업).
 *
 * 흐름: send-code(중복확인 후 코드 발송) → verify-code(코드 확인 → 세션에 인증완료 기록) → 가입 시 isVerified 검사.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final MemberValidationService memberValidationService;
    private final EmailService emailService;

    private static final String K_CODE          = "EMAIL_VERIFY_CODE";
    private static final String K_TARGET        = "EMAIL_VERIFY_TARGET";
    private static final String K_EXPIRE        = "EMAIL_VERIFY_EXPIRE_AT";
    private static final String K_VERIFIED_MAIL = "EMAIL_VERIFIED_MAIL";

    /** 인증코드 유효시간(ms) */
    private static final long CODE_TTL_MS = 5 * 60 * 1000L;

    private final SecureRandom secureRandom = new SecureRandom();

    public enum SendResult { SENT, DUPLICATE, INVALID }
    public enum VerifyResult { OK, NO_CODE, MISMATCH, EXPIRED, WRONG }

    /**
     * 인증코드 발송. 이메일 중복 확인을 먼저 하고, 통과 시에만 코드를 발송·저장한다.
     * (메일 발송이 성공한 뒤에 세션에 저장하여, 발송 실패 시 세션이 오염되지 않게 함)
     */
    public SendResult sendCode(String email, HttpSession session) {
        if (email == null || email.isBlank()) {
            return SendResult.INVALID;
        }
        if (memberValidationService.existsByEmail(email)) {
            return SendResult.DUPLICATE;
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        emailService.sendVerificationCodeEmail(email, code); // 실패 시 RuntimeException → 호출 측에서 처리

        session.setAttribute(K_CODE, code);
        session.setAttribute(K_TARGET, email);
        session.setAttribute(K_EXPIRE, System.currentTimeMillis() + CODE_TTL_MS);
        session.removeAttribute(K_VERIFIED_MAIL); // 새 코드 발송 시 이전 인증완료 무효화
        return SendResult.SENT;
    }

    /** 인증코드 확인. 성공 시 세션에 인증완료 이메일을 기록한다. */
    public VerifyResult verifyCode(String email, String code, HttpSession session) {
        Object savedCode = session.getAttribute(K_CODE);
        Object target    = session.getAttribute(K_TARGET);
        Object expireAt  = session.getAttribute(K_EXPIRE);

        if (savedCode == null || target == null || expireAt == null) {
            return VerifyResult.NO_CODE;
        }
        if (!target.equals(email)) {
            return VerifyResult.MISMATCH;
        }
        if (System.currentTimeMillis() > (long) expireAt) {
            return VerifyResult.EXPIRED;
        }
        if (!savedCode.equals(code)) {
            return VerifyResult.WRONG;
        }

        session.setAttribute(K_VERIFIED_MAIL, email);
        session.removeAttribute(K_CODE); // 1회용
        return VerifyResult.OK;
    }

    /** 해당 이메일이 현재 세션에서 인증완료 상태인지 */
    public boolean isVerified(String email, HttpSession session) {
        Object verified = session.getAttribute(K_VERIFIED_MAIL);
        return email != null && email.equals(verified);
    }

    /** 인증 관련 세션 정보 정리(가입 완료 후 호출) */
    public void clear(HttpSession session) {
        session.removeAttribute(K_CODE);
        session.removeAttribute(K_TARGET);
        session.removeAttribute(K_EXPIRE);
        session.removeAttribute(K_VERIFIED_MAIL);
    }
}
