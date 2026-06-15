package com.example.java.member.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.entity.EmailToken;
import com.example.java.member.entity.Member;
import com.example.java.member.repository.EmailTokenRepository;
import com.example.java.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final MemberRepository memberRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RememberMeTokenService rememberMeTokenService;

    /**
     * 아이디 + 이메일로 회원 조회 후 PW_RESET 토큰 발급 & 메일 발송
     * 일치하는 회원이 없으면 조용히 리턴 (이메일 존재 여부 노출 방지)
     */
    @Transactional
    public void sendResetEmail(String username, String email) {
        Member member = memberRepository.findByUsernameAndEmail(username, email)
                .orElse(null);
        if (member == null) return;

        // 기존 미사용 PW_RESET 토큰 무효화
        emailTokenRepository.findByMemberAndPurposeAndUsedYn(member, "PW_RESET", "N")
                .forEach(EmailToken::markUsed);

        // 평문 토큰 생성 (UUID)
        String rawToken = UUID.randomUUID().toString().replace("-", "");

        // SHA-256 해시 저장
        String tokenHash = sha256(rawToken);

        emailTokenRepository.save(EmailToken.builder()
                .member(member)
                .tokenHash(tokenHash)
                .purpose("PW_RESET")
                .expireAt(LocalDateTime.now().plusHours(1))
                .build());

        emailService.sendPasswordResetEmail(email, rawToken);
    }

    /**
     * 토큰 유효성 검증
     * 유효하면 해당 Member 반환, 아니면 예외
     */
    @Transactional(readOnly = true)
    public Member validateToken(String rawToken) {
        String tokenHash = sha256(rawToken);

        EmailToken emailToken = emailTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 링크입니다."));

        if (emailToken.isUsed()) {
            throw new IllegalArgumentException("이미 사용된 링크입니다.");
        }
        if (emailToken.isExpired()) {
            throw new IllegalArgumentException("만료된 링크입니다. 다시 요청해 주세요.");
        }

        Member member = emailToken.getMember();
        member.getUsername(); // 트랜잭션 내부에서 proxy 강제 초기화
        return member;
    }

    /**
     * 비밀번호 변경 후 토큰 사용 처리
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = sha256(rawToken);

        EmailToken emailToken = emailTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 링크입니다."));

        if (emailToken.isUsed()) {
            throw new IllegalArgumentException("이미 사용된 링크입니다.");
        }
        if (emailToken.isExpired()) {
            throw new IllegalArgumentException("만료된 링크입니다. 다시 요청해 주세요.");
        }

        Member member = emailToken.getMember();
        member.updatePassword(passwordEncoder.encode(newPassword));
        emailToken.markUsed();

        // 비밀번호 변경 시 기존 자동로그인 토큰 전체 무효화 (탈취 대비)
        rememberMeTokenService.invalidateAll(member.getSeq());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("토큰 해싱 오류", e);
        }
    }
}
