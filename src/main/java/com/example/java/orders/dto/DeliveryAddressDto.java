package com.example.java.orders.dto;

public record DeliveryAddressDto(
        Long seq,
        String alias,
        String defaultYn,
        String recipientName,
        String recipientPhone,
        String zipcode,
        String address,
        String addressDetail
) {
}