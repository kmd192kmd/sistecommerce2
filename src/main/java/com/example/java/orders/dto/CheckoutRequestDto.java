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
     * 현재 테스트 단계에서는 coupon.seq를 직접 받는다.
     *
     * 테스트 쿠폰:
     * - 80: 결제 테스트 10% 할인 쿠폰
     * - 81: 결제 테스트 3000원 할인 쿠폰
     *
     * TODO 실제 연동 시 교체
     * - couponSeq가 아니라 memberCouponSeq를 받는 것이 좋다.
     * - orders.member_coupon_seq에 연결하려면 member_coupon.seq가 필요하다.
     */
    private Long couponSeq;

    /**
     * CARD / TRANSFER / VIRTUAL_ACCOUNT
     */
    private String paymentMethod;

    /**
     * 현재 화면에서 넘어오는 임시 주문번호.
     *
     * 실제 주문 생성 시에는 서버에서 새 orderUid를 생성한다.
     */
    private String orderUid;

    /**
     * 화면에서 넘어온 금액.
     *
     * 주의:
     * 이 값은 그대로 믿으면 안 된다.
     * 주문 생성 시 서버에서 다시 계산해야 한다.
     */
    private Integer amount;

    private Boolean agreeRequired;
}