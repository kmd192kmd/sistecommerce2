package com.example.java.orders.dto;

public record CheckoutItemDto(
        Long optionsSeq,
        Long productSeq,
        String name,
        String image,
        String option,
        int price,
        int quantity
) {
    public int totalPrice() {
        return price * quantity;
    }
}