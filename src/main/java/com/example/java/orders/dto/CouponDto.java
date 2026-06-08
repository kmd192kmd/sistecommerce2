package com.example.java.orders.dto;

public record CouponDto(
        Long seq,
        String name,
        Integer discountType,
        Integer discountPrice,
        Integer discountRate,
        String discountText
) {
}