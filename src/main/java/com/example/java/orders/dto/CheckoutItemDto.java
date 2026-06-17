package com.example.java.orders.dto;

public record CheckoutItemDto(
        Long optionsSeq,
        Long productSeq,
        String name,
        String image,
        String option,

        /**
         * 상품 1개 원가.
         * product.price + options.additionalPrice
         */
        int originalPrice,

        /**
         * 상품 1개당 핫딜 할인금액.
         */
        int hotdealDiscount,

        /**
         * 핫딜 적용 후 상품 1개 가격.
         */
        int price,

        int quantity
) {
    /**
     * 원가 기준 상품 합계.
     */
    public int originalTotalPrice() {
        return originalPrice * quantity;
    }

    /**
     * 핫딜 할인 총액.
     */
    public int hotdealDiscountTotal() {
        return hotdealDiscount * quantity;
    }

    /**
     * 핫딜 적용 후 상품 합계.
     */
    public int totalPrice() {
        return price * quantity;
    }
}