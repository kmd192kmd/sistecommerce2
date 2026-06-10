package com.example.java.product.scheduler;

import com.example.java.product.service.ProductListService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularProductsScheduler {

    private final ProductListService productListService;

    // 애플리케이션 시작 시 1회 캐시를 선행 로드합니다 (Cache Warming)
    @PostConstruct
    public void initCache() {
        try {
            log.info("인기 상품 캐시 워밍 시작...");
            productListService.refreshPopularProducts();
            log.info("인기 상품 캐시 워밍 완료.");
        } catch (Exception e) {
            log.error("인기 상품 캐시 워밍 중 오류 발생: ", e);
        }
    }

    // 매 시간 정각마다 캐시를 백그라운드에서 조용히 갱신합니다 (사용자 응답속도 지연 없음)
    @Scheduled(cron = "0 0 * * * *")
    public void schedulePopularProductsRefresh() {
        try {
            log.info("인기 상품 캐시 갱신 스케줄러 실행...");
            productListService.refreshPopularProducts();
            log.info("인기 상품 캐시 갱신 완료.");
        } catch (Exception e) {
            log.error("인기 상품 캐시 갱신 스케줄러 실행 중 오류 발생: ", e);
        }
    }
}
