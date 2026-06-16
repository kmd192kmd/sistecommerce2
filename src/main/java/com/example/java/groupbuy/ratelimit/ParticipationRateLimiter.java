package com.example.java.groupbuy.ratelimit;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 공구 참여 신청 rate limiter (점유 고갈 DoS 억제).
 *
 * <p>토스 2단계 결제 모델 때문에 "결제 전 점유"가 생긴다(참여 신청 시점에 occupy).
 * 이를 악용해 결제는 안 하고 참여 신청만 반복해 점유를 선점하는 남용이 가능하다.
 * 같은 주체(계정/IP)의 반복을 Redis fixed-window 카운터로 억제한다.
 *
 * <p>한계: 분산 IP + 신선 계정으로 각 1회씩 하는 "최초 대량 점유"는 막지 못한다
 * (행위가 정상 참여와 동일). 이는 잔여 위험으로 인정하고 측정 기반으로 대응한다.
 * 자세한 배경은 로컬 문서 {@code 쇼핑몰프로젝트용자료/공구_점유고갈_DoS_트러블슈팅_2026-06-16.md} 참조.
 *
 * <p>정책: 계정·IP 각각 1분 내 {@link #MAX_ATTEMPTS}회까지 허용, 초과하면 차단(윈도우 만료까지).
 * 둘 중 하나라도 초과하면 차단한다.
 */
@Component
@RequiredArgsConstructor
public class ParticipationRateLimiter {

    /** Redis 키 접두사 (네임스페이스 shop:* 컨벤션). */
    private static final String KEY_PREFIX = "shop:ratelimit:gb-participate:";
    /** 윈도우 내 허용 최대 시도 횟수. 이 값을 넘는 순간부터 차단. */
    private static final long MAX_ATTEMPTS = 5;
    /** fixed-window 길이. */
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    /**
     * 참여 시도 1회를 기록하고 차단 여부를 반환한다.
     *
     * <p>부수효과: 호출할 때마다 계정·IP 카운터를 모두 +1 한다.
     * 둘 중 하나라도 한도({@link #MAX_ATTEMPTS})를 넘으면 {@code true}(차단)를 반환한다.
     *
     * @param memberSeq 참여를 시도한 회원 번호
     * @param ip        요청 클라이언트 IP
     * @return true면 rate limit 초과(차단), false면 허용
     */
    public boolean isBlocked(long memberSeq, String ip) {
        boolean memberOver = hit("member:" + memberSeq);
        boolean ipOver = hit("ip:" + ip);
        return memberOver || ipOver;
    }

    /** 키 카운터를 +1 한다. 첫 증가일 때만 윈도우 TTL을 건다(fixed window). 한도 초과면 true. */
    private boolean hit(String suffix) {
        String key = KEY_PREFIX + suffix;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, WINDOW);
        }
        return count != null && count > MAX_ATTEMPTS;
    }
}
