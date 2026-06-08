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

    public List<CheckoutItemDto> getCheckoutItems() {
        List<CheckoutItemDto> items = ordersQueryRepository.findCheckoutItemsByTestOptionsSeq();

        if (items.isEmpty()) {
            throw new IllegalStateException("테스트 결제 상품을 찾을 수 없습니다.");
        }

        return items;
    }

    public List<CouponDto> getCoupons() {
        return ordersQueryRepository.findTestCoupons();
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

    public PriceSummaryDto getPriceSummary(Long couponSeq) {
        List<CheckoutItemDto> items = getCheckoutItems();

        int productTotalPrice = items.stream()
                .mapToInt(CheckoutItemDto::totalPrice)
                .sum();

        int deliveryFee = productTotalPrice >= 30000 ? 0 : 3000;
        int hotdealDiscount = 0;

        CouponDto selectedCoupon = ordersQueryRepository.findTestCoupon(couponSeq);
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

        if ("0".equals(coupon.discountType())) {
            if (coupon.discountRate() == null) {
                return 0;
            }

            return productTotalPrice * coupon.discountRate() / 100;
        }

        if ("1".equals(coupon.discountType())) {
            if (coupon.discountPrice() == null) {
                return 0;
            }

            return Math.min(productTotalPrice, coupon.discountPrice());
        }

        return 0;
    }
}