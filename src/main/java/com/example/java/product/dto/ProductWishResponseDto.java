package com.example.java.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 찜 처리 결과 응답 DTO
 */
@Getter
@AllArgsConstructor
public class ProductWishResponseDto {

    /**
     * 현재 찜 상태
     *
     * true  = 찜한 상태
     * false = 찜 취소 상태
     */
    private boolean wished;

    /**
     * 처리 결과 메시지
     */
    private String message;
}