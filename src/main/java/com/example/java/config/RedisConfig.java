package com.example.java.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

/**
 * Redis 설정.
 *
 * 네임스페이스 분리 전략:
 *   - Spring Session  → "shop:session:*"  (application.yml spring.session.redis.namespace)
 *   - 추후 캐시 용도  → "shop:cache:*"   (별도 CacheConfig에서 keyPrefix 지정)
 *   - 추후 채팅 용도  → "shop:chat:*"    (별도 ChatRedisConfig에서 keyPrefix 지정)
 *
 * Bean 이름을 "sessionRedis"로 명시해 다른 RedisTemplate Bean과 충돌을 방지.
 * 다른 도메인에서 RedisTemplate이 필요하면 @Qualifier("chatRedis") 등 별도 Bean을 추가할 것.
 *
 * @EnableRedisIndexedHttpSession: principal name 인덱스를 생성해
 * FindByIndexNameSessionRepository.findByPrincipalName() 사용 가능.
 */
@Configuration
@EnableRedisIndexedHttpSession(redisNamespace = "shop:session")
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    /**
     * 세션 관리용 RedisTemplate.
     * key/value 모두 String 직렬화 — 세션 attribute는 Spring Session이 직접 처리.
     */
    @Bean("sessionRedis")
    public RedisTemplate<String, String> sessionRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
