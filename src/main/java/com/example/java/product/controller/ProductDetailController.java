package com.example.java.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.service.ProductDetailService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/*
    ProductDetailController

    상품 상세 보기, 등록, 수정, 삭제(CRUD) 및 관련 API 처리를 담당합니다.
*/
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductDetailController {

    private final ProductDetailService productDetailService;

    // 상품 상세 화면
    @GetMapping("/{seq}")
    public String getProductDetail(
            @PathVariable("seq") Long seq,
            HttpSession session,
            Model model) {

        Long memberSeq = getLoginMemberSeq(session);
        ProductDto product = productDetailService.getProductDetail(seq, memberSeq);

        model.addAttribute("product", product);
        model.addAttribute("loginMemberSeq", memberSeq);

        return "product/detail";
    }

    // 상품 등록 폼
    @GetMapping("/register")
    public String registerForm(Model model) {

        model.addAttribute("productDto", new ProductDto());

        return "product/register";
    }

    // 상품 등록 처리
    @PostMapping("/register")
    public String createProduct(ProductDto dto) {

        productDetailService.createProduct(dto);

        return "redirect:/product/list";
    }

    // 상품 수정 폼
    @GetMapping("/edit/{seq}")
    public String editForm(
            @PathVariable("seq") Long seq,
            Model model) {

        ProductDto product = productDetailService.getProductWithoutViewCount(seq);
        model.addAttribute("productDto", product);

        return "product/edit";
    }

    // 상품 수정 처리
    @PostMapping("/edit/{seq}")
    public String updateProduct(
            @PathVariable("seq") Long seq,
            ProductDto dto) {

        productDetailService.updateProduct(seq, dto);

        return "redirect:/product/view/" + seq;
    }

    // 상품 삭제 처리 (HTML Form 용)
    @PostMapping("/delete/{seq}")
    public String deleteProduct(
            @PathVariable("seq") Long seq) {

        productDetailService.deleteProduct(seq);

        return "redirect:/product/list";
    }

    // 상품 삭제 API (AJAX 용)
    @DeleteMapping("/api/product/{productSeq}")
    public ResponseEntity<?> deleteProductApi(
            @PathVariable(name = "productSeq") Long productSeq) {

        boolean result = productDetailService.deleteProduct(productSeq);

        if (!result) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok("상품이 삭제되었습니다.");
    }

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
