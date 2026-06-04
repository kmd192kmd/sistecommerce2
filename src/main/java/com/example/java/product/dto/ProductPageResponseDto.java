package com.example.java.product.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPageResponseDto {

    private int page;
    private int size;

    private int totalCount;
    private int totalPages;

    private List<ProductDto> products;
}