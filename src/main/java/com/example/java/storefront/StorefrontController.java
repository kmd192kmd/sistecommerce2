package com.example.java.storefront;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.product.dto.CategoryDto;
import com.example.java.product.dto.ProductDto;
import com.example.java.product.service.CategoryService;
import com.example.java.product.service.ProductService;

import lombok.RequiredArgsConstructor;

/**
 * 스토어프론트(고객) 화면 라우팅.
 * DB 연동을 적용하여 실제 상품 데이터를 조회하도록 처리했습니다.
 */
@Controller
@RequiredArgsConstructor
public class StorefrontController {

    private final ProductService productService;
    private final CategoryService categoryService;

    // 쇼핑몰 목록 (SHOP-PRD-01)
    @GetMapping("/products")
    public String products(
            @RequestParam(value = "categorySeq", required = false) Long categorySeq,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "price", required = false) String price,
            @RequestParam(value = "rating", required = false) String rating,
            @RequestParam(value = "hideOutOfStock", required = false) Boolean hideOutOfStock,
            jakarta.servlet.http.HttpSession session,
            Model model) {
        
        // 최근 검색어 세션 저장
        if (keyword != null && !keyword.trim().isEmpty()) {
            @SuppressWarnings("unchecked")
            List<String> recentKeywords = (List<String>) session.getAttribute("recentKeywords");
            if (recentKeywords == null) {
                recentKeywords = new java.util.ArrayList<>();
            }
            recentKeywords.remove(keyword);
            recentKeywords.add(0, keyword);
            if (recentKeywords.size() > 10) {
                recentKeywords.remove(recentKeywords.size() - 1);
            }
            session.setAttribute("recentKeywords", recentKeywords);
        }

        // 정렬 파라미터 매핑 (Thymeleaf 템플릿의 sort 값과 ProductService의 sortBy 매핑)
        String sortBy = "salesCount";
        if (sort != null) {
            sortBy = switch (sort) {
                case "priceAsc" -> "price_asc";
                case "priceDesc" -> "price_desc";
                case "newest" -> "newest";
                case "ratingDesc" -> "rating";
                default -> "salesCount";
            };
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
        Page<ProductDto> productPage = productService.getProductList(categorySeq, keyword, sortBy, page, minPrice, maxPrice, minRating, hideStockVal);
        List<ProductDto> productList = productPage.getContent();
        
        // 추천 상품 목록 (실제 DB 평점 높은 순 상위 5개 추천)
        List<ProductDto> recommendedList = productService.getProductList(null, null, "rating", 0)
                .getContent().stream().limit(5).collect(java.util.stream.Collectors.toList());

        // 최근 조회 상품 목록 (세션에 저장된 상품 ID 순서대로 가져오기, 없으면 인기 조회 상품 대체)
        @SuppressWarnings("unchecked")
        List<Long> recentViewedSeqs = (List<Long>) session.getAttribute("recentViewedSeqs");
        List<ProductDto> recentViewedList = new java.util.ArrayList<>();
        if (recentViewedSeqs != null && !recentViewedSeqs.isEmpty()) {
            for (Long rSeq : recentViewedSeqs) {
                try {
                    ProductDto p = productService.getProductWithoutViewCount(rSeq);
                    // 삭제되었거나 접근 제한된 상품 필터링
                    if (!"DELETED".equals(p.getStatus()) && !"Y".equals(p.getHideYn()) && !"STOPPED".equals(p.getSaleStatus())) {
                        recentViewedList.add(p);
                    }
                } catch (Exception e) {
                    // 없는 상품은 건너뜀
                }
            }
        }
        if (recentViewedList.isEmpty()) {
            // 최근 본 상품이 없을 시 인기 상품(조회수 기준)으로 임시 노출
            recentViewedList = productService.getProductList(null, null, "salesCount", 0)
                    .getContent().stream().limit(5).collect(java.util.stream.Collectors.toList());
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
        model.addAttribute("productList", productList); // HTML 템플릿의 ${productList}에 대응!
        model.addAttribute("recommendedList", recommendedList);
        model.addAttribute("recentViewedList", recentViewedList);
        model.addAttribute("productPage", productPage);
        
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        
        return "product/list";
    }

    // 상품 상세 (SHOP-PRD-02)
    @GetMapping("/products/{seq}")
    public String productDetail(@PathVariable long seq, jakarta.servlet.http.HttpSession session, Model model) {
        ProductDto product = productService.getProductDetail(seq);
        model.addAttribute("product", product);

        // 최근 본 상품 세션 등록
        @SuppressWarnings("unchecked")
        List<Long> recentViewedSeqs = (List<Long>) session.getAttribute("recentViewedSeqs");
        if (recentViewedSeqs == null) {
            recentViewedSeqs = new java.util.ArrayList<>();
        }
        recentViewedSeqs.remove(seq); // 이미 존재하면 중복 방지를 위해 삭제
        recentViewedSeqs.add(0, seq); // 최신 본 상품을 리스트 최상단에 배치
        if (recentViewedSeqs.size() > 5) {
            recentViewedSeqs.remove(recentViewedSeqs.size() - 1); // 최대 5개 노출
        }
        session.setAttribute("recentViewedSeqs", recentViewedSeqs);

        return "product/detail";
    }

    // 공동구매 목록 (GB-01)
    @GetMapping("/group-buys")
    public String groupBuys(Model model) {
        model.addAttribute("groupBuys", SampleProducts.groupBuys());
        return "groupbuy/list";
    }

    // 공동구매 상세 (GB-02) — 동적 영역은 React mount (A방식)
    @GetMapping("/group-buys/{seq}")
    public String groupBuyDetail(@PathVariable long seq, Model model) {
        model.addAttribute("groupBuy", SampleProducts.groupBuy(seq));
        return "groupbuy/detail";
    }

    // 장바구니 (CART-MAIN-01)
    @GetMapping("/cart")
    public String cart(Model model) {
        model.addAttribute("cartItems", SampleProducts.cartItems());
        return "cart/cart";
    }

    // 결제
    @GetMapping("/order/checkout")
    public String checkout(Model model) {
        model.addAttribute("cartItems", SampleProducts.cartItems());
        return "order/checkout";
    }

    // 최근 검색어 개별 삭제
    @GetMapping("/products/recent-search/delete")
    public String deleteRecentKeyword(@RequestParam("keyword") String keyword, jakarta.servlet.http.HttpSession session) {
        @SuppressWarnings("unchecked")
        List<String> recentKeywords = (List<String>) session.getAttribute("recentKeywords");
        if (recentKeywords != null) {
            recentKeywords.remove(keyword);
            session.setAttribute("recentKeywords", recentKeywords);
        }
        return "redirect:/products";
    }

    // 최근 검색어 전체 삭제
    @GetMapping("/products/recent-search/clear")
    public String clearRecentKeywords(jakarta.servlet.http.HttpSession session) {
        session.removeAttribute("recentKeywords");
        return "redirect:/products";
    }
}
