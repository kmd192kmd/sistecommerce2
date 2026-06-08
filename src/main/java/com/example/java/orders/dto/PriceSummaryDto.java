package com.example.java.orders.dto;

public record PriceSummaryDto(
        int productTotalPrice,
        int deliveryFee,
        int couponDiscount,
        int hotdealDiscount,
        int finalPrice
) {
}