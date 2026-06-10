package com.example.java.product.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.product.dto.ProductWishRequestDto;
import com.example.java.product.dto.ProductWishResponseDto;
import com.example.java.product.service.ProductWishService;

import lombok.RequiredArgsConstructor;

/**
 * 상품 찜 API Controller
 *
 * 상품 상세 화면에서 하트 버튼을 클릭하면
 * 상품 기준으로 찜 등록 / 찜 취소를 처리합니다.
 */
@RestController
@RequiredArgsConstructor
public class ProductWishController {

    private final ProductWishService productWishService;

    /**
     * 상품 찜 등록 / 취소 API
     *
     * 요청 주소:
     * POST /api/product/wish
     */
    @PostMapping("/api/product/wish")
    public ResponseEntity<?> toggleWish(
            @RequestBody ProductWishRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        /*
            로그인하지 않은 경우

            Spring Security에서 로그인 정보가 없으면
            userDetails가 null로 들어옵니다.
        */
        if (userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        /*
            현재 로그인한 회원 번호

            여기서 getMemberSeq() 부분이 빨간 줄이면
            CustomUserDetails.java 안에 있는 회원번호 getter 이름에 맞춰 바꿔야 합니다.
        */
        Long memberSeq = userDetails.getMemberSeq();

        /*
            화면에서 넘어온 상품 번호
        */
        Long productSeq = requestDto.getProductSeq();

        /*
            찜 등록 / 찜 취소 처리
        */
        ProductWishResponseDto result =
                productWishService.toggleWish(memberSeq, productSeq);

        return ResponseEntity.ok(result);
    }

    /**
     * 현재 상품 찜 여부 확인 API
     *
     * 요청 주소:
     * GET /api/product/wish?productSeq=1451
     */
    @GetMapping("/api/product/wish")
    public ResponseEntity<?> checkWish(
            @RequestParam("productSeq") Long productSeq,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        /*
            로그인하지 않은 사용자는 찜한 상태가 아니므로 false 반환
        */
        if (userDetails == null) {
            return ResponseEntity.ok(false);
        }

        /*
            현재 로그인한 회원 번호
        */
        Long memberSeq = userDetails.getMemberSeq();

        /*
            현재 상품을 찜했는지 확인
        */
        boolean wished = productWishService.isWished(memberSeq, productSeq);

        return ResponseEntity.ok(wished);
    }
}