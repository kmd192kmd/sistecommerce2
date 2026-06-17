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
import com.example.java.member.repository.MemberRepository;
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
 * - 휴면/정지/탈퇴(status != 1) 계정은 자동로그인을 차단한다.
 * - 유효기간은 발급/회전 시점 기준 슬라이딩(기본 14일).
 *
 * 검증→상태확인→회전을 {@link #consume(String, String, String)} 단일 트랜잭션으로 처리해
 * detached 엔티티 변경 누락 및 LazyInitialization 문제를 피한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RememberMeTokenService {

    private final RememberMeTokenRepository rememberMeTokenRepository;
    private final MemberRepository memberRepository;

    /** 정상(활성) 회원 status 값 */
    private static final int STATUS_ACTIVE = 1;
    /** 토큰 유효기간 (슬라이딩) */
    private static final Duration VALIDITY = Duration.ofDays(14);
    /** 원본 토큰 바이트 수 (Base64url 인코딩 시 ~43자) */
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 로그인 성공 시 신규 토큰 발급.
     *
     * @return 쿠키에 담아 내려보낼 원본 토큰 (DB에는 해시만 저장됨)
     */
    @Transactional
    public String issue(Long memberSeq, String deviceType, String deviceFingerprint) {
        Member member = memberRepository.getReferenceById(memberSeq); // FK 참조용 프록시
        return issueInternal(member, deviceType, deviceFingerprint);
    }

    /**
     * 쿠키 원본 토큰 소비(검증 + 상태확인 + 회전)를 단일 트랜잭션으로 처리.
     *
     * @return 성공 시 username + 새 원본 토큰. 실패 시 {@code success=false}.
     */
    @Transactional
    public AutoLoginResult consume(String rawToken, String deviceType, String deviceFingerprint) {
        if (rawToken == null || rawToken.isBlank()) {
            return AutoLoginResult.fail("EMPTY");
        }

        Optional<RememberMeToken> opt = rememberMeTokenRepository.findByTokenHash(sha256(rawToken));
        if (opt.isEmpty()) {
            return AutoLoginResult.fail("NOT_FOUND");
        }

        RememberMeToken token = opt.get();

        // 이미 사용된 토큰의 재제출 → 탈취 의심 → 해당 회원 토큰 전체 무효화
        if (token.isUsed()) {
            log.warn("사용된 자동로그인 토큰 재제출 감지 - 회원 토큰 전체 무효화. memberSeq: {}",
                    token.getMember().getSeq());
            rememberMeTokenRepository.deleteByMemberSeq(token.getMember().getSeq());
            return AutoLoginResult.fail("THEFT");
        }

        if (token.isExpired()) {
            return AutoLoginResult.fail("EXPIRED");
        }

        Member member = token.getMember(); // 트랜잭션 내부 → lazy 초기화 안전
        if (member.getStatus() == null || member.getStatus() != STATUS_ACTIVE) {
            // 휴면/정지/탈퇴 계정은 자동로그인 차단
            return AutoLoginResult.fail("INACTIVE");
        }

        token.markUsed(); // 영속 상태 → 커밋 시 update flush
        String newRawToken = issueInternal(member, deviceType, deviceFingerprint);

        return AutoLoginResult.success(member.getUsername(), newRawToken);
    }

    /** 특정 회원의 모든 토큰 무효화 (로그아웃·비밀번호 변경·탈취 의심) */
    @Transactional
    public void invalidateAll(Long memberSeq) {
        int deleted = rememberMeTokenRepository.deleteByMemberSeq(memberSeq);
        log.debug("자동로그인 토큰 전체 무효화 - memberSeq: {}, deleted: {}", memberSeq, deleted);
    }

    // -------------------------------------------------------------------------

    private String issueInternal(Member member, String deviceType, String deviceFingerprint) {
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

    /**
     * 자동로그인 소비 결과.
     *
     * @param success     성공 여부
     * @param username    성공 시 인증할 회원 username
     * @param newRawToken 성공 시 쿠키에 다시 심을 새 원본 토큰
     * @param reason      실패 사유 (EMPTY/NOT_FOUND/THEFT/EXPIRED/INACTIVE)
     */
    public record AutoLoginResult(boolean success, String username, String newRawToken, String reason) {
        public static AutoLoginResult success(String username, String newRawToken) {
            return new AutoLoginResult(true, username, newRawToken, null);
        }
        public static AutoLoginResult fail(String reason) {
            return new AutoLoginResult(false, null, null, reason);
        }
    }
}
