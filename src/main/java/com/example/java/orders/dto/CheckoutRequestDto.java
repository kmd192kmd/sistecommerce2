package com.example.java.orders.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class CheckoutRequestDto {

    /**
     * 장바구니에서 선택한 cart.seq 목록.
     */
    private List<Long> cartSeq;

    private Long deliveryAddressSeq;

    private String deliveryType;

    private String recipientName;

    private String recipientPhone;

    private String zipcode;

    private String address;

    private String addressDetail;

    private Boolean saveDeliveryAddress;

    private String requestMemo;

    private Long memberCouponSeq;

    private String paymentMethod;

    private String orderUid;

    private Integer amount;

    private Boolean agreeRequired;

    private Boolean directBuyYn;

    private Boolean directBuy;

    private Long optionsSeq;

    private Integer quantity;
}