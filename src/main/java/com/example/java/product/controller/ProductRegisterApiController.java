package com.example.java.product.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.product.dto.ProductCreateRequestDto;
import com.example.java.product.dto.ProductCreateResponseDto;
import com.example.java.product.service.ProductRegisterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProductRegisterApiController {

    private final ProductRegisterService productRegisterService;


    /*
        상품 등록 API

        테스트 주소:
        POST http://localhost:8080/api/product/register

        화면에서 입력받는다고 생각한 방식입니다.

        form 태그 예시:
        <form method="post"
              action="/api/product/register"
              enctype="multipart/form-data">

        상품명, 가격, 설명, 옵션 등:
        - ProductCreateRequestDto dto로 받습니다.

        이미지 파일:
        - images라는 name으로 받습니다.

        대표 이미지:
        - thumbnailIndex 값으로 몇 번째 이미지를 대표 이미지로 할지 결정합니다.
        - 기본값 0은 첫 번째 이미지를 대표 이미지로 사용한다는 뜻입니다.
    */
    @PostMapping(
            value = "/api/product/register",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> registerProduct(
            @ModelAttribute ProductCreateRequestDto dto,
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam(name = "detailImages", required = false) List<MultipartFile> detailImages,
            @RequestParam(name = "thumbnailIndex", defaultValue = "0") int thumbnailIndex,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        /*
            로그인하지 않은 경우

            상품 등록은 현재 로그인한 회원 번호를
            판매자 번호로 사용하기 때문에 로그인이 필요합니다.
        */
        if (customUserDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("로그인 후 상품을 등록할 수 있습니다.");
        }

        /*
            현재 로그인한 회원 번호

            화면에서 sellerSeq를 받지 않고,
            Spring Security의 로그인 사용자 정보에서 회원 번호를 가져옵니다.
        */
        Long memberSeq = customUserDetails.getMemberSeq();

        /*
            판매자 번호 설정

            ProductRegisterService에서는 dto.getSellerSeq()를 기준으로
            판매자 존재 여부를 검증하고 product 테이블에 저장합니다.

            따라서 화면에서 sellerSeq를 보내지 않아도,
            실제 로그인한 회원 번호가 판매자 번호로 들어갑니다.
        */
        dto.setSellerSeq(memberSeq);

        ProductCreateResponseDto response =
                productRegisterService.createProduct(dto, images, detailImages, thumbnailIndex);

        return ResponseEntity.ok(response);
    }


    /*
        입력값 오류 처리

        Service에서 IllegalArgumentException이 발생하면
        400 Bad Request로 응답합니다.
    */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {

        return ResponseEntity
                .badRequest()
                .body(e.getMessage());
    }


    /*
        예상하지 못한 서버 오류 처리
    */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {

        return ResponseEntity
                .internalServerError()
                .body("서버 오류가 발생했습니다: " + e.getMessage());
    }
}