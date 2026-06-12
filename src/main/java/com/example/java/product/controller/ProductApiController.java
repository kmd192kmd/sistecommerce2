package com.example.java.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.service.ProductDetailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductApiController {

    private final ProductDetailService productDetailService;

    /*
        상품 상세 조회 테스트용 API

        요청 주소:
        GET http://localhost:8080/api/product/상품번호
    */
    @GetMapping("/{seq}")
    public ResponseEntity<?> detail(@PathVariable("seq") Long productSeq) {

        try {
            ProductDto product = productDetailService.getProductDetail(productSeq, null);
            return ResponseEntity.ok(product);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(404)
                    .body("상품을 찾을 수 없습니다. 상품번호 또는 상품 상태값을 확인하세요.");

        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }
}