package com.example.java.product.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCreateRequestDto {

    private Long productSeq;

    private Long memberSeq;

    private Long orderItemSeq;

    private Integer rating;

    private String content;
}