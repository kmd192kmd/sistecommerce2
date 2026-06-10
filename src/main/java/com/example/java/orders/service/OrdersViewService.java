package com.example.java.orders.service;

import com.example.java.member.entity.DeliveryAddress;
import com.example.java.member.service.MemberAddressService;
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
    private final MemberAddressService memberAddressService;

    public List<CheckoutItemDto> getCheckoutItems(Long memberSeq) {
        List<CheckoutItemDto> items =
                ordersQueryRepository.findCheckoutItemsByMemberCart(memberSeq);

        if (items.isEmpty()) {
            throw new IllegalStateException("장바구니에 결제할 상품이 없습니다.");
        }

        return items;
    }

    public List<CouponDto> getCoupons(Long memberSeq) {
        return ordersQueryRepository.findAvailableCouponsByMemberSeq(memberSeq);
    }

    /**
     * 로그인 회원이 DB에 저장한 배송지 목록.
     */
    public List<DeliveryAddressDto> getDeliveryAddresses(Long memberSeq) {
        return memberAddressService.myAddress(memberSeq)
                .stream()
                .map(this::toDeliveryAddressDto)
                .toList();
    }

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

    private DeliveryAddressDto toDeliveryAddressDto(DeliveryAddress address) {
        return new DeliveryAddressDto(
                address.getSeq(),
                address.getAddressAlias(),
                address.getDefaultYn(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getZipcode(),
                address.getAddress(),
                address.getAddressDetail()
        );
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