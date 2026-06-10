package com.example.java.orders.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CheckoutRequestDto {

    private Long deliveryAddressSeq;

    /**
     * EXISTING: 기존 배송지 사용
     * NEW: 새 배송지 직접 입력
     */
    private String deliveryType;

    private String recipientName;

    private String recipientPhone;

    private String zipcode;

    private String address;

    private String addressDetail;

    private Boolean saveDeliveryAddress;

    private String requestMemo;

    /**
     * 회원이 실제로 발급받은 쿠폰 번호.
     *
     * 주의:
     * coupon.seq가 아니라 member_coupon.seq를 받는다.
     */
    private Long memberCouponSeq;

    /**
     * CARD / TRANSFER / VIRTUAL_ACCOUNT
     */
    private String paymentMethod;

    /**
     * 화면에서 넘어오는 임시 주문번호.
     * 실제 주문 생성 시에는 서버에서 새 orderUid를 생성한다.
     */
    private String orderUid;

    /**
     * 화면에서 넘어온 금액.
     * 실제 주문 생성 시에는 이 값을 믿지 않고 서버에서 다시 계산한다.
     */
    private Integer amount;

    private Boolean agreeRequired;
}