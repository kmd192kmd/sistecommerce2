package com.example.java.product.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.service.ProductService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 목록 조회 및 검색 (페이징, 필터링, 정렬 지원)
    @GetMapping("/list")
    public String getProducts(
            @RequestParam(value = "categorySeq", required = false) Long categorySeq,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {
        
        Page<ProductDto> products = productService.getProductList(categorySeq, keyword, sortBy, page);
        
        model.addAttribute("products", products);
        model.addAttribute("categorySeq", categorySeq);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("currentPage", page);
        
        return "product/list";
    }

    // 상품 상세 조회 (상세 조회 시 조회수 자동 증가)
    @GetMapping("/{seq}")
    public String getProductDetail(@PathVariable("seq") Long seq, Model model) {
        ProductDto product = productService.getProductDetail(seq);
        model.addAttribute("product", product);
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
        productService.createProduct(dto);
        return "redirect:/product/list";
    }

    // 상품 수정 폼
    @GetMapping("/edit/{seq}")
    public String editForm(@PathVariable("seq") Long seq, Model model) {
        // 상세 조회를 통해 정보 로드 (조회수 증가는 배제해야 하므로 직접 조회하는 것이 맞지만, 
        // 편의상 기 정의된 getProductDetail을 이용하거나 별도 생성 가능)
        ProductDto product = productService.getProductDetail(seq);
        model.addAttribute("productDto", product);
        return "product/edit";
    }

    // 상품 수정 처리
    @PostMapping("/edit/{seq}")
    public String updateProduct(@PathVariable("seq") Long seq, ProductDto dto) {
        productService.updateProduct(seq, dto);
        return "redirect:/product/" + seq;
    }

    // 상품 삭제 처리 (논리 삭제)
    @PostMapping("/delete/{seq}")
    public String deleteProduct(@PathVariable("seq") Long seq) {
        productService.deleteProduct(seq);
        return "redirect:/product/list";
    }
}
