package com.example.java.product.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.service.CategoryService;
import com.example.java.product.service.ProductDetailService;
import com.example.java.product.service.ProductListService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/*
    ProductListController

    상품 목록 화면 조회, 검색, 필터링, 정렬 및 최근 검색어 관리를 처리합니다.
*/
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductListController {

    private final ProductListService productListService;
    private final ProductDetailService productDetailService;
    private final CategoryService categoryService;

    // 쇼핑몰 목록 화면
    @GetMapping("")
    public String products(
            @RequestParam(value = "categorySeq", required = false) Long categorySeq,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "price", required = false) String price,
            @RequestParam(value = "rating", required = false) String rating,
            @RequestParam(value = "hideOutOfStock", required = false) Boolean hideOutOfStock,
            HttpServletRequest request,
            HttpSession session,
            Model model) {

        // 최근 검색어 세션 저장
        if (keyword != null && !keyword.trim().isEmpty()) {
            @SuppressWarnings("unchecked")
            List<String> recentKeywords = (List<String>) session.getAttribute("recentKeywords");
            if (recentKeywords == null) {
                recentKeywords = new ArrayList<>();
            }
            recentKeywords.remove(keyword);
            recentKeywords.add(0, keyword);
            if (recentKeywords.size() > 5) {
                recentKeywords = new ArrayList<>(recentKeywords.subList(0, 5));
            }
            session.setAttribute("recentKeywords", recentKeywords);
        }

        // 가격 필터 파싱 (예: "0-10000", "10000-50000" 등)
        Integer minPrice = null;
        Integer maxPrice = null;
        if (price != null && !price.equals("all") && !price.trim().isEmpty()) {
            String[] parts = price.split("-");
            if (parts.length == 2) {
                try {
                    minPrice = Integer.parseInt(parts[0]);
                    if (parts[1].equals("max")) {
                        maxPrice = 999999999;
                    } else {
                        maxPrice = Integer.parseInt(parts[1]);
                    }
                } catch (NumberFormatException e) {
                    // 무시
                }
            }
        }

        // 별점 필터 파싱 (예: "4" -> 4점 이상, "3" -> 3점 이상)
        Double minRating = null;
        if (rating != null && !rating.trim().isEmpty()) {
            try {
                minRating = Double.parseDouble(rating);
            } catch (NumberFormatException e) {
                // 무시
            }
        }
        boolean hideStockVal = hideOutOfStock != null && hideOutOfStock;
        Page<ProductDto> productPage = productListService.getProductList(categorySeq, keyword, sort, page, minPrice, maxPrice, minRating, hideStockVal);
        List<ProductDto> productList = productPage.getContent();

        // 추천 상품 목록 (캐시된 5개 추천 상품 가져오기)
        List<ProductDto> recommendedList = productListService.getPopularProducts();

        // 최근 조회 상품 목록 (세션에 저장된 상품 ID 순서대로 가져오기)
        @SuppressWarnings("unchecked")
        List<Long> recentViewedSeqs = (List<Long>) session.getAttribute("recentViewedSeqs");
        List<ProductDto> recentViewedList = new ArrayList<>();
        if (recentViewedSeqs != null && !recentViewedSeqs.isEmpty()) {
            for (Long rSeq : recentViewedSeqs) {
                try {
                    ProductDto p = productDetailService.getProductWithoutViewCount(rSeq);
                    recentViewedList.add(p);
                } catch (Exception e) {
                    // 없는 상품은 건너뜀
                }
            }
        }

        // 10개 단위 페이징 블록 계산 (고정 블록 방식)
        int totalPages = productPage.getTotalPages();
        int currentPage = productPage.getNumber();
        int pageBlockSize = 10;
        int startPage = 0;
        int endPage = 0;
        if (totalPages > 0) {
            startPage = (currentPage / pageBlockSize) * pageBlockSize;
            endPage = Math.min(totalPages - 1, startPage + pageBlockSize - 1);
        }

        model.addAttribute("keyword", keyword);
        model.addAttribute("categorySeq", categorySeq);
        model.addAttribute("categoryPath", categoryService.getCategoryPath(categorySeq));
        model.addAttribute("sort", sort);
        model.addAttribute("price", price); // 파라미터 보존
        model.addAttribute("rating", rating); // 파라미터 보존
        model.addAttribute("hideOutOfStock", hideStockVal); // 품절 숨김 보존
        model.addAttribute("productList", productList);
        model.addAttribute("recommendedList", recommendedList);
        model.addAttribute("recentViewedList", recentViewedList);
        model.addAttribute("productPage", productPage);

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return "product/list :: #product-main-container";
        }
        return "product/list";
    }

    // 최근 검색어 개별 삭제
    @GetMapping("/recent-search/delete")
    @ResponseBody
    public ResponseEntity<Void> deleteRecentKeyword(@RequestParam("keyword") String keyword, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<String> recentKeywords = (List<String>) session.getAttribute("recentKeywords");
        if (recentKeywords != null) {
            recentKeywords.remove(keyword);
            session.setAttribute("recentKeywords", recentKeywords);
        }
        return ResponseEntity.ok().build();
    }

    // 최근 검색어 전체 삭제
    @GetMapping("/recent-search/clear")
    @ResponseBody
    public ResponseEntity<Void> clearRecentKeywords(HttpSession session) {
        session.removeAttribute("recentKeywords");
        return ResponseEntity.ok().build();
    }
}
