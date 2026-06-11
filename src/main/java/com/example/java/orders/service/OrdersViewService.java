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

    public List<CheckoutItemDto> getCheckoutItems(Long memberSeq, List<Long> cartSeqList) {
        List<CheckoutItemDto> items =
                ordersQueryRepository.findCheckoutItemsByMemberCart(memberSeq, cartSeqList);

        if (items.isEmpty()) {
            throw new IllegalStateException("선택한 장바구니 상품이 없거나 결제할 수 없는 상품입니다.");
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

    public PriceSummaryDto getPriceSummary(Long memberSeq,
                                           Long memberCouponSeq,
                                           List<Long> cartSeqList) {

        List<CheckoutItemDto> items = getCheckoutItems(memberSeq, cartSeqList);

        /*
            상품 원가 총합.
            핫딜 적용 전 금액이다.
         */
        int productTotalPrice = items.stream()
                .mapToInt(CheckoutItemDto::originalTotalPrice)
                .sum();

        /*
            핫딜 할인 총액.
         */
        int hotdealDiscount = items.stream()
                .mapToInt(CheckoutItemDto::hotdealDiscountTotal)
                .sum();

        /*
            핫딜 적용 후 상품금액.
         */
        int afterHotdealProductPrice = productTotalPrice - hotdealDiscount;

        if (afterHotdealProductPrice < 0) {
            afterHotdealProductPrice = 0;
        }

        /*
            배송비도 핫딜 적용 후 금액 기준으로 판단.
         */
        int deliveryFee = afterHotdealProductPrice >= 30000 ? 0 : 3000;

        CouponDto selectedCoupon =
                ordersQueryRepository.findAvailableCouponByMemberSeqAndMemberCouponSeq(
                        memberSeq,
                        memberCouponSeq
                );

        /*
            쿠폰 할인은 핫딜 적용 후 상품금액 기준으로 계산.
         */
        int couponDiscount = calculateCouponDiscount(afterHotdealProductPrice, selectedCoupon);

        int finalPrice = afterHotdealProductPrice + deliveryFee - couponDiscount;

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