package com.example.java.orders.dto;

public record OrderCreateResultDto(
        Long ordersSeq,
        String orderUid,
        Integer finalPrice
) {
}