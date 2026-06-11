package com.example.java.member.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.entity.Member;
import com.example.java.member.entity.RememberMeToken;
import com.example.java.member.repository.RememberMeTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 자동로그인(remember-me) 토큰 생명주기 서비스.
 *
 * 정책
 * - 원본 토큰은 쿠키로만 전달하고, DB에는 SHA-256 해시(token_hash)만 저장한다.
 * - 1회용 회전 토큰: 자동로그인에 성공하면 기존 토큰을 used 처리하고 새 토큰을 발급한다.
 * - 이미 사용된 토큰이 재제출되면 탈취 의심 → 해당 회원의 모든 토큰을 무효화한다.
 * - 유효기간은 발급/회전 시점 기준 슬라이딩(기본 14일).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RememberMeTokenService {

    private final RememberMeTokenRepository rememberMeTokenRepository;

    /** 토큰 유효기간 (슬라이딩) */
    private static final Duration VALIDITY = Duration.ofDays(14);
    /** 원본 토큰 바이트 수 (Base64url 인코딩 시 ~43자) */
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 신규 토큰 발급.
     *
     * @return 쿠키에 담아 내려보낼 원본 토큰 (DB에는 해시만 저장됨)
     */
    @Transactional
    public String issue(Member member, String deviceType, String deviceFingerprint) {
        String rawToken = generateRawToken();

        RememberMeToken token = RememberMeToken.builder()
                .member(member)
                .tokenHash(sha256(rawToken))
                .deviceType(safe(deviceType, 100, "UNKNOWN"))      // NOT NULL 컬럼
                .deviceFingerprint(truncate(deviceFingerprint, 200))
                .expireAt(LocalDateTime.now().plus(VALIDITY))
                .usedYn(RememberMeToken.USED_N)
                .build();

        rememberMeTokenRepository.save(token);
        log.debug("자동로그인 토큰 발급 - memberSeq: {}, deviceType: {}",
                member.getSeq(), token.getDeviceType());
        return rawToken;
    }

    /**
     * 쿠키 원본 토큰 검증.
     *
     * @return 유효한 토큰 엔티티. 무효/만료/탈취의심이면 null.
     */
    @Transactional
    public RememberMeToken validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        Optional<RememberMeToken> opt = rememberMeTokenRepository.findByTokenHash(sha256(rawToken));
        if (opt.isEmpty()) {
            return null;
        }

        RememberMeToken token = opt.get();

        // 이미 사용된 토큰의 재제출 → 탈취 의심 → 해당 회원 토큰 전체 무효화
        if (token.isUsed()) {
            log.warn("사용된 자동로그인 토큰 재제출 감지 - 회원 토큰 전체 무효화. memberSeq: {}",
                    token.getMember().getSeq());
            rememberMeTokenRepository.deleteByMemberSeq(token.getMember().getSeq());
            return null;
        }

        if (token.isExpired()) {
            return null;
        }

        return token;
    }

    /**
     * 토큰 회전: 기존 토큰을 used 처리하고 새 토큰을 발급한다.
     *
     * @return 새로 발급된 원본 토큰
     */
    @Transactional
    public String rotate(RememberMeToken oldToken, String deviceType, String deviceFingerprint) {
        oldToken.markUsed();   // 영속 상태 → dirty checking 으로 update
        return issue(oldToken.getMember(), deviceType, deviceFingerprint);
    }

    /** 특정 회원의 모든 토큰 무효화 (로그아웃·비밀번호 변경·탈취 의심) */
    @Transactional
    public void invalidateAll(Long memberSeq) {
        int deleted = rememberMeTokenRepository.deleteByMemberSeq(memberSeq);
        log.debug("자동로그인 토큰 전체 무효화 - memberSeq: {}, deleted: {}", memberSeq, deleted);
    }

    // -------------------------------------------------------------------------

    /** SecureRandom 기반 원본 토큰 생성 (Base64url, 패딩 없음) */
    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 해시 (hex, 64자) */
    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 미지원 환경", e);
        }
    }

    /** null/공백이면 기본값, 길이 초과면 잘라냄 */
    private String safe(String value, int maxLen, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return truncate(value, maxLen);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
