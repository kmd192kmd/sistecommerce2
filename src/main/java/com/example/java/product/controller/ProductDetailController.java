package com.example.java.product.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.service.CategoryService;
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
    private final CategoryService categoryService;

    // 상품 상세 화면
    @GetMapping("/{seq}")
    public String getProductDetail(
            @PathVariable("seq") Long seq,
            HttpSession session,
            Model model) {

        Long memberSeq = getLoginMemberSeq(session);

        System.out.println("session memberSeq = " + session.getAttribute("memberSeq"));
        System.out.println("loginMemberSeq = " + memberSeq);

        ProductDto product = productDetailService.getProductDetail(seq, memberSeq);

        // 최근 조회 상품 세션 저장
        @SuppressWarnings("unchecked")
        List<Long> recentViewedSeqs = (List<Long>) session.getAttribute("recentViewedSeqs");

        if (recentViewedSeqs == null) {
            recentViewedSeqs = new ArrayList<>();
        }

        recentViewedSeqs.remove(seq);
        recentViewedSeqs.add(0, seq);

        if (recentViewedSeqs.size() > 5) {
            recentViewedSeqs = new ArrayList<>(recentViewedSeqs.subList(0, 5));
        }

        session.setAttribute("recentViewedSeqs", recentViewedSeqs);

        model.addAttribute("product", product);

        /*
            로그인 회원 번호

            리뷰 등록, 수정, 삭제 시
            화면 JavaScript에서 현재 로그인 회원 번호를 사용할 수 있도록 내려줍니다.
        */
        model.addAttribute("loginMemberSeq", memberSeq);

        return "product/detail";
    }

    /*
        상품 등록 폼

        접속 주소:
        GET /products/register
    */
    @GetMapping("/register")
    public String registerForm(Model model) {

        model.addAttribute("productDto", new ProductDto());

        /*
            카테고리 전체 목록

            depthLevel
            0 = 대분류
            1 = 중분류
            2 = 소분류
        */
        model.addAttribute("categories", categoryService.getAllCategories());

        return "product/register";
    }

    // 상품 등록 처리
    @PostMapping("/register")
    public String createProduct(ProductDto dto) {

        productDetailService.createProduct(dto);

        return "redirect:/products";
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

        return "redirect:/products/" + seq;
    }

    /*
        상품 삭제 처리

        접속 주소:
        POST /products/delete/{seq}

        화면 form에서 삭제 버튼을 눌렀을 때 사용합니다.
    */
    @PostMapping("/delete/{seq}")
    public String deleteProduct(
            @PathVariable("seq") Long seq) {

        productDetailService.deleteProduct(seq);

        return "redirect:/products";
    }

    /*
        상품 삭제 API

        접속 주소:
        DELETE /products/api/product/{productSeq}

        실제 DELETE가 아니라
        status = 'DELETED',
        hide_yn = 'Y'
        로 변경하는 논리 삭제 방식입니다.
    */
    @DeleteMapping("/api/product/{productSeq}")
    public ResponseEntity<?> deleteProductApi(
            @PathVariable(name = "productSeq") Long productSeq) {

        boolean result = productDetailService.deleteProduct(productSeq);

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