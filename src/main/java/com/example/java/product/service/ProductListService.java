package com.example.java.product.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.entity.Product;
import com.example.java.product.repository.ProductListRepository;

import lombok.RequiredArgsConstructor;

/*
    ProductListService

    상품 목록 조회, 검색, 필터링 등 읽기 중심의 목록 관련 기능을 제공합니다.
*/
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductListService {

    private final ProductListRepository productListRepository;
    private final CategoryService categoryService;
    private final org.springframework.cache.CacheManager cacheManager;

    public Page<ProductDto> getProductList(
            Long categorySeq,
            String keyword,
            String sortBy,
            int page) {

        return getProductList(categorySeq, keyword, sortBy, page, null, null, null, false);
    }

    public Page<ProductDto> getProductList(
            Long categorySeq,
            String keyword,
            String sortBy,
            int page,
            Integer minPrice,
            Integer maxPrice,
            Double minRating,
            boolean hideOutOfStock) {

        Pageable pageable = PageRequest.of(page, 20, getSortOption(sortBy));

        Integer finalMinPrice = minPrice != null ? minPrice : 0;
        Integer finalMaxPrice = maxPrice != null ? maxPrice : 999999999;
        Double finalMinRating = minRating != null ? minRating : 0.0;
        String finalKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : null;
        String saleStatus = hideOutOfStock ? "ON_SALE" : null;

        List<Long> categorySeqs = categoryService.getDescendantCategorySeqs(categorySeq);

        Page<Product> productPage = productListRepository.findWithFilters(
                categorySeqs,
                finalKeyword,
                finalMinPrice,
                finalMaxPrice,
                finalMinRating,
                saleStatus,
                pageable
        );

        return productPage.map(Product::toDto);
    }

    private Sort getSortOption(String sortBy) {

        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, "salesCount");
        }

        return switch (sortBy.toLowerCase()) {
            case "price_asc", "priceasc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc", "pricedesc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdDate");
            case "review", "reviewdesc" -> Sort.by(Sort.Direction.DESC, "reviewCount");
            case "recommend" -> Sort.by(Sort.Direction.DESC, "recommend");
            default -> Sort.by(Sort.Direction.DESC, "salesCount");
        };
    }

    @org.springframework.cache.annotation.Cacheable(value = "popularProducts", key = "'list'")
    public List<ProductDto> getPopularProducts() {
        return getProductList(null, null, "recommend", 0)
                .getContent().stream().limit(5).collect(java.util.stream.Collectors.toList());
    }

    @org.springframework.cache.annotation.CachePut(value = "popularProducts", key = "'list'")
    public List<ProductDto> refreshPopularProducts() {
        // 1. 기존 캐시에서 이전 데이터 순위 가져오기
        java.util.List<ProductDto> previousList = null;
        org.springframework.cache.Cache cache = cacheManager.getCache("popularProducts");
        if (cache != null) {
            org.springframework.cache.Cache.ValueWrapper wrapper = cache.get("list");
            if (wrapper != null) {
                previousList = (java.util.List<ProductDto>) wrapper.get();
            }
        }

        // 2. 신규 추천 상품 조회 (5개)
        java.util.List<ProductDto> newProducts = getProductList(null, null, "recommend", 0)
                .getContent().stream().limit(5).collect(java.util.stream.Collectors.toList());

        // 3. 이전 순위 맵 구성 (ProductSeq -> Rank(1부터 시작))
        java.util.Map<Long, Integer> previousRankMap = new java.util.HashMap<>();
        if (previousList != null) {
            for (int i = 0; i < previousList.size(); i++) {
                if (previousList.get(i) != null && previousList.get(i).getSeq() != null) {
                    previousRankMap.put(previousList.get(i).getSeq(), i + 1);
                }
            }
        }

        // 4. 순위 변동 계산 및 설정
        int maxRise = 0;
        for (int i = 0; i < newProducts.size(); i++) {
            ProductDto newProduct = newProducts.get(i);
            int newRank = i + 1;
            newProduct.setHot(false); // 초기화

            int rise = 0;
            if (previousRankMap.containsKey(newProduct.getSeq())) {
                int oldRank = previousRankMap.get(newProduct.getSeq());
                int change = oldRank - newRank; // 이전 순위가 더 크면(예: 3위 -> 1위) 순위 상승(+)
                rise = change;
                if (change > 0) {
                    newProduct.setRankChange("▲" + change);
                } else if (change < 0) {
                    newProduct.setRankChange("▼" + Math.abs(change));
                } else {
                    newProduct.setRankChange("-");
                }
            } else {
                newProduct.setRankChange("NEW");
                // 이전 리스트가 존재하는 경우에만 신규 진입 상품의 이전 순위를 6위로 가정하여 상승폭 계산
                if (previousList != null && !previousList.isEmpty()) {
                    rise = 6 - newRank;
                } else {
                    rise = 0;
                }
            }

            if (rise > maxRise) {
                maxRise = rise;
            }
        }

        // 5. 가장 많이 상승한 상품에 hot=true 설정 (상승폭이 1 이상인 경우에만)
        if (maxRise > 0) {
            for (int i = 0; i < newProducts.size(); i++) {
                ProductDto newProduct = newProducts.get(i);
                int newRank = i + 1;
                int rise = 0;
                if (previousRankMap.containsKey(newProduct.getSeq())) {
                    rise = previousRankMap.get(newProduct.getSeq()) - newRank;
                } else if (previousList != null && !previousList.isEmpty()) {
                    rise = 6 - newRank;
                }
                if (rise == maxRise) {
                    newProduct.setHot(true);
                }
            }
        }

        return newProducts;
    }
}
