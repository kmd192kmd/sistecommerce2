package com.example.java.product.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<ProductCreateResponseDto> registerProduct(
            @ModelAttribute ProductCreateRequestDto dto,
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam(name = "detailImages", required = false) List<MultipartFile> detailImages,
            @RequestParam(name = "thumbnailIndex", defaultValue = "0") int thumbnailIndex) {

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