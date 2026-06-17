package com.example.java.admin.controller;

import com.example.java.admin.dto.AdminSellerProductDto;
import com.example.java.admin.service.AdminSellerService;
import com.example.java.product.entity.Seller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/seller")
@RequiredArgsConstructor
public class AdminSellerController {

    private final AdminSellerService adminSellerService;

    // 1. 판매처 목록 조회 (이름 기반 검색 지원)
    @GetMapping("/list")
    public String getSellerList(@RequestParam(value = "name", required = false) String name, Model model) {
        List<Seller> sellers = adminSellerService.searchSellers(name);
        model.addAttribute("sellers", sellers);
        model.addAttribute("keyword", name);
        return "admin/seller/list"; // 향후 생성할 HTML 화면 경로
    }

    // 2. 판매처별 상품 리스트 및 남은 재고량 조회 API (이름 검색 지원)
    @GetMapping("/{sellerSeq}/products")
    @ResponseBody
    public ResponseEntity<List<AdminSellerProductDto>> getProductsBySeller(
            @PathVariable("sellerSeq") Long sellerSeq,
            @RequestParam(value = "productName", required = false) String productName) {
        List<AdminSellerProductDto> products = adminSellerService.getSellerProducts(sellerSeq, productName);
        return ResponseEntity.ok(products);
    }

    // 3. 상품 상세페이지 내 상품 상태(status) 변경 API
    @PostMapping("/product/{productSeq}/status")
    @ResponseBody
    public ResponseEntity<String> changeProductStatus(
            @PathVariable("productSeq") Long productSeq,
            @RequestParam("status") String status) {
        adminSellerService.changeProductStatus(productSeq, status);
        return ResponseEntity.ok("상품 상태가 성공적으로 변경되었습니다.");
    }
}
