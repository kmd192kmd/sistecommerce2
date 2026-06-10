package com.example.java.orders.service;

import com.example.java.orders.dto.CheckoutItemDto;
import com.example.java.orders.dto.CouponDto;
import com.example.java.orders.dto.DeliveryAddressDto;
import com.example.java.orders.dto.OrderPreviewDto;
import com.example.java.orders.dto.PriceSummaryDto;
import com.example.java.orders.repository.OrdersQueryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdersViewService {

    private final OrdersQueryRepository ordersQueryRepository;

    /**
     * 로그인 회원의 장바구니 상품 목록.
     */
    public List<CheckoutItemDto> getCheckoutItems(Long memberSeq) {
        List<CheckoutItemDto> items =
                ordersQueryRepository.findCheckoutItemsByMemberCart(memberSeq);

        if (items.isEmpty()) {
            throw new IllegalStateException("장바구니에 결제할 상품이 없습니다.");
        }

        return items;
    }

    /**
     * 로그인 회원이 실제 발급받은 사용 가능한 쿠폰 목록.
     */
    public List<CouponDto> getCoupons(Long memberSeq) {
        return ordersQueryRepository.findAvailableCouponsByMemberSeq(memberSeq);
    }

    public List<DeliveryAddressDto> getDeliveryAddresses() {
        return List.of(
                new DeliveryAddressDto(
                        1L,
                        "집",
                        "Y",
                        "홍길동",
                        "010-1234-5678",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "골드아파트 101동 1001호"
                ),
                new DeliveryAddressDto(
                        2L,
                        "회사",
                        "N",
                        "홍길동",
                        "010-1234-5678",
                        "04524",
                        "서울특별시 중구 세종대로 110",
                        "12층"
                )
        );
    }

    /**
     * 서버 기준 최종 결제금액 계산.
     *
     * 상품 금액은 로그인 회원의 장바구니 기준으로 계산한다.
     * memberCouponSeq는 coupon.seq가 아니라 member_coupon.seq다.
     */
    public PriceSummaryDto getPriceSummary(Long memberSeq, Long memberCouponSeq) {
        List<CheckoutItemDto> items = getCheckoutItems(memberSeq);

        int productTotalPrice = items.stream()
                .mapToInt(CheckoutItemDto::totalPrice)
                .sum();

        int deliveryFee = productTotalPrice >= 30000 ? 0 : 3000;
        int hotdealDiscount = 0;

        CouponDto selectedCoupon =
                ordersQueryRepository.findAvailableCouponByMemberSeqAndMemberCouponSeq(
                        memberSeq,
                        memberCouponSeq
                );

        int couponDiscount = calculateCouponDiscount(productTotalPrice, selectedCoupon);

        int finalPrice = productTotalPrice + deliveryFee - couponDiscount - hotdealDiscount;

        if (finalPrice < 0) {
            finalPrice = 0;
        }

        return new PriceSummaryDto(
                productTotalPrice,
                deliveryFee,
                couponDiscount,
                hotdealDiscount,
                finalPrice
        );
    }

    public OrderPreviewDto getOrderPreview() {
        return new OrderPreviewDto("GM-" + System.currentTimeMillis());
    }

    private int calculateCouponDiscount(int productTotalPrice, CouponDto coupon) {
        if (coupon == null) {
            return 0;
        }

        if (Integer.valueOf(0).equals(coupon.discountType())) {
            if (coupon.discountRate() == null) {
                return 0;
            }

            return productTotalPrice * coupon.discountRate() / 100;
        }

        if (Integer.valueOf(1).equals(coupon.discountType())) {
            if (coupon.discountPrice() == null) {
                return 0;
            }

            return Math.min(productTotalPrice, coupon.discountPrice());
        }

        return 0;
    }
}