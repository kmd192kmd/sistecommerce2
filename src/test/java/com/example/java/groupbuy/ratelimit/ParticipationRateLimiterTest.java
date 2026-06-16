package com.example.java.groupbuy.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 공구 참여 rate limiter 단위 검증 (Redis는 Mockito로 대체).
 *
 * fixed-window 카운터 동작만 검증하므로 실제 Redis 없이 increment 반환값을 모킹한다.
 * 정책: 계정·IP 각각 1분 5회까지 허용, 초과(6회째)부터 차단. 둘 중 하나라도 넘으면 차단.
 */
@ExtendWith(MockitoExtension.class)
class ParticipationRateLimiterTest {

    private static final String MEMBER_KEY = "shop:ratelimit:gb-participate:member:1";
    private static final String IP_KEY = "shop:ratelimit:gb-participate:ip:10.0.0.1";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @InjectMocks
    private ParticipationRateLimiter rateLimiter;

    @Test
    @DisplayName("1분 내 5회까지는 허용하고 6회째부터 차단한다")
    void 한도_초과시_차단한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // 계정·IP 카운터가 호출마다 1→2→...→6 으로 증가한다고 가정
        when(valueOps.increment(MEMBER_KEY)).thenReturn(1L, 2L, 3L, 4L, 5L, 6L);
        when(valueOps.increment(IP_KEY)).thenReturn(1L, 2L, 3L, 4L, 5L, 6L);

        for (int i = 1; i <= 5; i++) {
            assertThat(rateLimiter.isBlocked(1L, "10.0.0.1")).isFalse();
        }
        assertThat(rateLimiter.isBlocked(1L, "10.0.0.1")).isTrue();
    }

    @Test
    @DisplayName("계정은 여유여도 IP만 한도를 넘으면 차단한다")
    void IP만_초과해도_차단한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(contains("member:"))).thenReturn(1L); // 계정은 여유
        when(valueOps.increment(contains("ip:"))).thenReturn(6L);     // IP만 초과

        assertThat(rateLimiter.isBlocked(1L, "10.0.0.1")).isTrue();
    }

    @Test
    @DisplayName("첫 증가일 때만 윈도우 TTL을 건다(fixed window)")
    void 첫_증가에만_TTL을_건다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // expire 반환값은 쓰지 않으므로 lenient stubbing
        lenient().when(redisTemplate.expire(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(valueOps.increment(MEMBER_KEY)).thenReturn(1L); // 첫 증가 → TTL 설정 대상
        when(valueOps.increment(IP_KEY)).thenReturn(2L);     // 두 번째 → TTL 설정 안 함

        rateLimiter.isBlocked(1L, "10.0.0.1");

        org.mockito.Mockito.verify(redisTemplate).expire(org.mockito.ArgumentMatchers.eq(MEMBER_KEY),
                org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(redisTemplate, org.mockito.Mockito.never())
                .expire(org.mockito.ArgumentMatchers.eq(IP_KEY), org.mockito.ArgumentMatchers.any());
    }
}
