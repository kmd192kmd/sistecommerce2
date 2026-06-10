package com.example.java.admin.dto;

import com.example.java.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminSellerProductDto {
    private Product product;
    private int totalStock;
}
