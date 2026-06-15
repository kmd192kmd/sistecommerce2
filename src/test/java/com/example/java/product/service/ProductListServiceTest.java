package com.example.java.product.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.repository.ProductListRepository;

@ExtendWith(MockitoExtension.class)
class ProductListServiceTest {

    @Mock
    private ProductListRepository productListRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private ValueWrapper valueWrapper;

    @Mock
    private org.springframework.data.elasticsearch.core.ElasticsearchOperations elasticsearchOperations;

    @Mock
    private com.example.java.admin.repository.HotDealProductRepository hotDealProductRepository;

    private ProductListService productListService;

    @BeforeEach
    void setUp() {
        productListService = Mockito.spy(new ProductListService(productListRepository, categoryService, cacheManager, elasticsearchOperations, hotDealProductRepository));
    }

    @Test
    void 인기상품_순위변동_및_HOT상품_선정_테스트_NEW_상승폭이_가장_클_때() {
        // Given: 이전 순위 설정 (1위: 101, 2위: 102, 3위: 103, 4위: 104, 5위: 105)
        List<ProductDto> previousList = new ArrayList<>();
        previousList.add(createProductDto(101L, "상품1"));
        previousList.add(createProductDto(102L, "상품2"));
        previousList.add(createProductDto(103L, "상품3"));
        previousList.add(createProductDto(104L, "상품4"));
        previousList.add(createProductDto(105L, "상품5"));

        when(cacheManager.getCache("popularProducts")).thenReturn(cache);
        when(cache.get("list")).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(previousList);

        // 신규 순위 설정
        // 1위: 103 (이전 3위 -> 상승폭: 2) -> ▲2
        // 2위: 101 (이전 1위 -> 하락)     -> ▼1
        // 3위: 106 (신규 진입 -> 가상 이전 순위 6위로 가정하여 상승폭: 6 - 3 = 3) -> NEW
        // 4위: 104 (이전 4위 -> 동일)     -> -
        // 5위: 102 (이전 2위 -> 하락)     -> ▼3
        List<ProductDto> newList = new ArrayList<>();
        newList.add(createProductDto(103L, "상품3"));
        newList.add(createProductDto(101L, "상품1"));
        newList.add(createProductDto(106L, "상품6"));
        newList.add(createProductDto(104L, "상품4"));
        newList.add(createProductDto(102L, "상품2"));

        Page<ProductDto> page = new PageImpl<>(newList);
        doReturn(page).when(productListService).getProductList(null, null, "recommend", 0, null, null, null, false, false);

        // When
        List<ProductDto> result = productListService.refreshPopularProducts();

        // Then
        assertEquals(5, result.size());

        // 순위 변동 문자열 검증
        assertEquals("▲2", result.get(0).getRankChange());
        assertEquals("▼1", result.get(1).getRankChange());
        assertEquals("NEW", result.get(2).getRankChange());
        assertEquals("-", result.get(3).getRankChange());
        assertEquals("▼3", result.get(4).getRankChange());

        // HOT 여부 검증 (가장 많이 상승한 3위 상품6(상승폭: 3)이 HOT이어야 함)
        assertFalse(result.get(0).isHot()); // 상품3 (+2)
        assertFalse(result.get(1).isHot()); // 상품1 (-1)
        assertTrue(result.get(2).isHot());  // 상품6 (+3) - NEW이자 최고 상승
        assertFalse(result.get(3).isHot()); // 상품4 (0)
        assertFalse(result.get(4).isHot()); // 상품2 (-3)
    }

    @Test
    void 인기상품_순위변동_및_HOT상품_선정_테스트_기존상품_상승폭이_가장_클_때() {
        // Given: 이전 순위 설정 (1위: 101, 2위: 102, 3위: 103, 4위: 104, 5위: 105)
        List<ProductDto> previousList = new ArrayList<>();
        previousList.add(createProductDto(101L, "상품1"));
        previousList.add(createProductDto(102L, "상품2"));
        previousList.add(createProductDto(103L, "상품3"));
        previousList.add(createProductDto(104L, "상품4"));
        previousList.add(createProductDto(105L, "상품5"));

        when(cacheManager.getCache("popularProducts")).thenReturn(cache);
        when(cache.get("list")).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(previousList);

        // 신규 순위 설정
        // 1위: 105 (이전 5위 -> 상승폭: 4) -> ▲4
        // 2위: 102 (이전 2위 -> 동일)     -> -
        // 3위: 107 (신규 진입 -> 상승폭: 6 - 3 = 3) -> NEW
        // 4위: 104 (이전 4위 -> 동일)     -> -
        // 5위: 101 (이전 1위 -> 하락)     -> ▼4
        List<ProductDto> newList = new ArrayList<>();
        newList.add(createProductDto(105L, "상품5"));
        newList.add(createProductDto(102L, "상품2"));
        newList.add(createProductDto(107L, "상품7"));
        newList.add(createProductDto(104L, "상품4"));
        newList.add(createProductDto(101L, "상품1"));

        Page<ProductDto> page = new PageImpl<>(newList);
        doReturn(page).when(productListService).getProductList(null, null, "recommend", 0, null, null, null, false, false);

        // When
        List<ProductDto> result = productListService.refreshPopularProducts();

        // Then
        assertEquals(5, result.size());

        // 순위 변동 문자열 검증
        assertEquals("▲4", result.get(0).getRankChange());
        assertEquals("-", result.get(1).getRankChange());
        assertEquals("NEW", result.get(2).getRankChange());
        assertEquals("-", result.get(3).getRankChange());
        assertEquals("▼4", result.get(4).getRankChange());

        // HOT 여부 검증 (상승폭 4단계인 상품5가 HOT이어야 함)
        assertTrue(result.get(0).isHot());  // 상품5 (+4) - 최고 상승
        assertFalse(result.get(1).isHot()); // 상품2 (0)
        assertFalse(result.get(2).isHot()); // 상품7 (+3)
        assertFalse(result.get(3).isHot()); // 상품4 (0)
        assertFalse(result.get(4).isHot()); // 상품1 (-4)
    }

    private ProductDto createProductDto(Long seq, String name) {
        ProductDto dto = new ProductDto();
        dto.setSeq(seq);
        dto.setProductName(name);
        return dto;
    }
}
