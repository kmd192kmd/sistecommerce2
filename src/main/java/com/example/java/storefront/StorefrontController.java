package com.example.java.storefront;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.product.dto.ProductDto;
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

    // 쇼핑몰 목록 (SHOP-PRD-01)
    @GetMapping("/products")
    public String products(
            @RequestParam(value = "categorySeq", required = false) Long categorySeq,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
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
        
        Page<ProductDto> productPage = productService.getProductList(categorySeq, keyword, sortBy, page);
        List<ProductDto> productList = productPage.getContent();
        
        // 추천 상품 목록 (평점 높은 순으로 상위 5개 임의 추출)
        List<ProductDto> recommendedList = productService.getProductList(null, null, "rating", 0)
                .getContent().stream().limit(5).collect(java.util.stream.Collectors.toList());

        // 최근 조회 상품 목록 (조회수 높은 순으로 상위 5개 임의 추출)
        List<ProductDto> recentViewedList = productService.getProductList(null, null, "salesCount", 0)
                .getContent().stream().limit(5).collect(java.util.stream.Collectors.toList());

        model.addAttribute("keyword", keyword);
        model.addAttribute("categorySeq", categorySeq);
        model.addAttribute("sort", sort);
        model.addAttribute("productList", productList); // HTML 템플릿의 ${productList}에 대응!
        model.addAttribute("recommendedList", recommendedList);
        model.addAttribute("recentViewedList", recentViewedList);
        
        // 페이징 처리를 위해 Page 객체 자체도 모델에 추가해 줍니다.
        model.addAttribute("productPage", productPage);
        
        return "product/list";
    }

    // 상품 상세 (SHOP-PRD-02)
    @GetMapping("/products/{seq}")
    public String productDetail(@PathVariable long seq, Model model) {
        ProductDto product = productService.getProductDetail(seq);
        model.addAttribute("product", product);
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
