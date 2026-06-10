package com.example.java.product.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 찜 요청 DTO
 *
 * 화면에서 서버로 상품 번호를 보낼 때 사용한다.
 */
@Getter
@Setter
public class ProductWishRequestDto {

    /**
     * 상품 번호
     */
    private Long productSeq;
}