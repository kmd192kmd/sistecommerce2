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
}
