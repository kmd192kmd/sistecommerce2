package com.example.java.product.scheduler;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.repository.ProductDetailRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductViewCountScheduler {

    private final StringRedisTemplate redisTemplate;
    private final ProductDetailRepository productDetailRepository;

    /**
     * 💡 10분마다 Redis에 쌓인 조회수 데이터를 읽어 DB를 일괄 업데이트합니다.
     */
    @Scheduled(fixedDelay = 600000) // 10분 (600,000ms)
    @Transactional
    public void writeBackViewCount() {
        // "product:view:count:*" 패턴에 매칭되는 모든 Redis 키를 가져옴
        Set<String> keys = redisTemplate.keys("product:view:count:*");
        
        if (keys == null || keys.isEmpty()) {
            return;
        }

        log.info("Redis 조회수 DB 동기화 시작. 대상 상품 수: {}건", keys.size());

        for (String key : keys) {
            // 키(product:view:count:15)에서 상품 ID(15)만 추출
            String productIdStr = key.replace("product:view:count:", "");
            Long productId = Long.parseLong(productIdStr);

            // Redis에서 쌓인 조회수 가져오기
            String countStr = redisTemplate.opsForValue().get(key);
            if (countStr != null) {
                long viewsToAdd = Long.parseLong(countStr);

                // 💡 단 한 번의 쿼리로 기존 조회수에 더해줌 (더티체킹 조회가 아닌 대량 갱신용 쿼리 권장)
                // UPDATE product SET view_count = view_count + :viewsToAdd WHERE seq = :productId
                productDetailRepository.incrementViewCount(productId, viewsToAdd);

                // DB 반영이 완료되었으므로 해당 Redis 카운트 키 삭제
                redisTemplate.delete(key);
            }
        }
        log.info("Redis 조회수 DB 동기화 완료.");
    }
}