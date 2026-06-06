package com.example.java.product.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.service.CategoryService;
import com.example.java.product.service.ProductService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    /*
        ProductService

        Controller는 DB에 직접 접근하지 않고,
        Service를 통해 상품 관련 기능을 실행합니다.

        흐름:
        Controller → Service → Repository → DB
    */
    private final ProductService productService;
    private final CategoryService categoryService;
    
    // 쇼핑몰 목록
    @GetMapping("")
    public String products(
            @RequestParam(value = "categorySeq", required = false) Long categorySeq,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "price", required = false) String price,
            @RequestParam(value = "rating", required = false) String rating,
            @RequestParam(value = "hideOutOfStock", required = false) Boolean hideOutOfStock,
            jakarta.servlet.http.HttpServletRequest request,
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
        
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return "product/list :: #product-main-container";
        }
        return "product/list";
    }
    
    // 최근 검색어 개별 삭제
    @GetMapping("/recent-search/delete")
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
    @GetMapping("/recent-search/clear")
    public String clearRecentKeywords(jakarta.servlet.http.HttpSession session) {
        session.removeAttribute("recentKeywords");
        return "redirect:/products";
    }
    
    @GetMapping("/{seq}")
    public String getProductDetail(
            @PathVariable("seq") Long seq,
            HttpSession session,
            Model model) {

        Long memberSeq = getLoginMemberSeq(session);

        ProductDto product = productService.getProductDetail(seq, memberSeq);

        model.addAttribute("product", product);

        return "product/detail";
    }


    /*
        상품 등록 폼

        접속 주소:
        GET /product/register
    */
    @GetMapping("/register")
    public String registerForm(Model model) {

        model.addAttribute("productDto", new ProductDto());

        return "product/register";
    }


    /*
        상품 등록 처리

        접속 주소:
        POST /product/register
    */
    @PostMapping("/register")
    public String createProduct(ProductDto dto) {

        productService.createProduct(dto);

        return "redirect:/product/list";
    }


    /*
        상품 수정 폼

        접속 주소:
        GET /product/edit/{seq}

        수정 화면에서는 조회수가 증가하면 안 되므로
        getProductWithoutViewCount()를 사용합니다.
    */
    @GetMapping("/edit/{seq}")
    public String editForm(
            @PathVariable("seq") Long seq,
            Model model) {

        ProductDto product = productService.getProductWithoutViewCount(seq);

        model.addAttribute("productDto", product);

        return "product/edit";
    }


    /*
        상품 수정 처리

        접속 주소:
        POST /product/edit/{seq}
    */
    @PostMapping("/edit/{seq}")
    public String updateProduct(
            @PathVariable("seq") Long seq,
            ProductDto dto) {

        productService.updateProduct(seq, dto);

        return "redirect:/product/view/" + seq;
    }


    /*
        상품 삭제 처리

        접속 주소:
        POST /product/delete/{seq}

        화면 form에서 삭제 버튼을 눌렀을 때 사용합니다.
    */
    @PostMapping("/delete/{seq}")
    public String deleteProduct(
            @PathVariable("seq") Long seq) {

        productService.deleteProduct(seq);

        return "redirect:/product/list";
    }


    /*
        상품 삭제 API

        접속 주소:
        DELETE /product/api/product/{productSeq}

        예:
        DELETE /product/api/product/1494

        실제 DELETE가 아니라
        status = 'DELETED',
        hide_yn = 'Y'
        로 변경하는 논리 삭제 방식입니다.
    */
    @DeleteMapping("/api/product/{productSeq}")
    public ResponseEntity<?> deleteProductApi(
            @PathVariable(name = "productSeq") Long productSeq) {

        boolean result = productService.deleteProduct(productSeq);

        if (!result) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok("상품이 삭제되었습니다.");
    }


    /*
        세션에서 로그인 회원번호 가져오기

        session에 "memberSeq"라는 이름으로 회원번호가 저장되어 있다고 가정합니다.

        만약 프로젝트에서 다른 이름을 사용하고 있다면
        "memberSeq" 부분만 바꾸면 됩니다.

        예:
        session.getAttribute("userSeq")
        session.getAttribute("loginMemberSeq")
        session.getAttribute("member")
    */
    private Long getLoginMemberSeq(HttpSession session) {

        Object value = session.getAttribute("memberSeq");

        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        if (value instanceof String) {
            return Long.parseLong((String) value);
        }

        return null;
    }
}