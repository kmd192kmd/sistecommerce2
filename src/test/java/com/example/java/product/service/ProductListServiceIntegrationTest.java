package com.example.java.product.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.example.java.product.dto.ProductDto;

@SpringBootTest(
    properties = {
        "spring.batch.job.enabled=false"
    }
)
class ProductListServiceIntegrationTest {

    @Autowired
    private ProductListService productListService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void 캐시연동_및_순위변동_통합_테스트() {
        Cache cache = cacheManager.getCache("popularProducts");
        assertNotNull(cache, "popularProducts 캐시 영역이 존재해야 합니다.");

        // 1. 깨끗한 테스트 상태를 위해 기존 캐시 비우기
        cache.evict("list");

        System.out.println("\n=======================================================");
        System.out.println("====== [1차 실행: 캐시 비어있음 (최초 로드 데이터)] ======");
        System.out.println("=======================================================");
        List<ProductDto> firstResult = productListService.refreshPopularProducts();
        
        // 캐시에서 데이터 정상 조회되는지 직접 검증
        Cache.ValueWrapper wrapper = cache.get("list");
        assertNotNull(wrapper, "1차 실행 후 캐시에 데이터가 저장되어 있어야 합니다.");
        
        List<ProductDto> cachedList1 = (List<ProductDto>) wrapper.get();
        assertNotNull(cachedList1);
        
        // 캐시에서 직접 가져온 값 화면에 출력
        for (int i = 0; i < cachedList1.size(); i++) {
            ProductDto p = cachedList1.get(i);
            System.out.println(String.format("순위: %d | 상품명: %s (Seq: %d) | 변동: %s | HOT: %b", 
                i + 1, p.getProductName(), p.getSeq(), p.getRankChange(), p.isHot()));
            assertEquals("NEW", p.getRankChange());
            assertFalse(p.isHot());
        }

        // 2차 실행을 위해 가상으로 캐시의 상품 순서를 조작하여 밀어넣음
        // DB 조회 결과 순위가 원래 [A, B, C, D, E]라면, 이전 순위를 [C, B, A, E, X(가상)]로 세팅
        // A (1위): 이전 3위 -> 현재 1위 (▲2) -> HOT
        // B (2위): 이전 2위 -> 현재 2위 (-)
        // C (3위): 이전 1위 -> 현재 3위 (▼2)
        // D (4위): 이전 없음 -> 현재 4위 (NEW, 상승폭: 6 - 4 = +2) -> HOT
        // E (5위): 이전 4위 -> 현재 5위 (▼1)
        if (cachedList1.size() >= 5) {
            List<ProductDto> fakePreviousList = new ArrayList<>();
            ProductDto pA = cachedList1.get(0); // A
            ProductDto pB = cachedList1.get(1); // B
            ProductDto pC = cachedList1.get(2); // C
            ProductDto pD = cachedList1.get(3); // D
            ProductDto pE = cachedList1.get(4); // E
            
            fakePreviousList.add(pC); // 1위
            fakePreviousList.add(pB); // 2위
            fakePreviousList.add(pA); // 3위
            fakePreviousList.add(pE); // 4위
            
            ProductDto fakeProduct = new ProductDto();
            fakeProduct.setSeq(999L);
            fakeProduct.setProductName("가상상품X");
            fakePreviousList.add(fakeProduct); // 5위
            
            // 캐시 수동 조작
            cache.put("list", fakePreviousList);
            
            System.out.println("\n=======================================================");
            System.out.println("====== [2차 실행: 캐시 조작 후 순위 변동 결과 조회] ======");
            System.out.println("=======================================================");
            List<ProductDto> secondResult = productListService.refreshPopularProducts();
            
            // 캐시에서 직접 가공된 순위 변동 결과 조회
            List<ProductDto> cachedList2 = (List<ProductDto>) cache.get("list").get();
            assertNotNull(cachedList2);
            
            // 캐시에서 직접 가져온 값 화면에 출력
            for (int i = 0; i < cachedList2.size(); i++) {
                ProductDto p = cachedList2.get(i);
                System.out.println(String.format("순위: %d | 상품명: %s (Seq: %d) | 변동: %s | HOT: %b", 
                    i + 1, p.getProductName(), p.getSeq(), p.getRankChange(), p.isHot()));
            }
            
            // A(index 0) 검증: ▲2, HOT=true
            assertEquals("▲2", cachedList2.get(0).getRankChange());
            assertTrue(cachedList2.get(0).isHot());
            
            // B(index 1) 검증: - , HOT=false
            assertEquals("-", cachedList2.get(1).getRankChange());
            assertFalse(cachedList2.get(1).isHot());
            
            // C(index 2) 검증: ▼2, HOT=false
            assertEquals("▼2", cachedList2.get(2).getRankChange());
            assertFalse(cachedList2.get(2).isHot());

            // D(index 3) 검증: NEW, HOT=true (가상 6위 -> 현재 4위 = 상승폭 2)
            assertEquals("NEW", cachedList2.get(3).getRankChange());
            assertTrue(cachedList2.get(3).isHot());

            // E(index 4) 검증: ▼1, HOT=false
            assertEquals("▼1", cachedList2.get(4).getRankChange());
            assertFalse(cachedList2.get(4).isHot());
        } else {
            System.out.println("\n[주의] 테스트에 필요한 실물 상품 데이터(5개 이상)가 DB에 없어서 2차 변동 검증은 생략합니다.");
        }
        System.out.println("=======================================================\n");
    }
}
