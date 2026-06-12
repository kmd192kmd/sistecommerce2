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
	
		return getPriceSummaryByItems(memberSeq, memberCouponSeq, items);
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
    
    public List<CheckoutItemDto> getDirectCheckoutItems(Long optionsSeq, Integer quantity) {
        CheckoutItemDto item =
                ordersQueryRepository.findCheckoutItemByOptionsSeq(optionsSeq, quantity);

        if (item == null) {
            throw new IllegalStateException("바로구매할 수 없는 상품이거나 재고가 부족합니다.");
        }

        return List.of(item);
    }

    public PriceSummaryDto getPriceSummaryByItems(Long memberSeq,
                                                  Long memberCouponSeq,
                                                  List<CheckoutItemDto> items) {

        int productTotalPrice = items.stream()
                .mapToInt(CheckoutItemDto::originalTotalPrice)
                .sum();

        int hotdealDiscount = items.stream()
                .mapToInt(CheckoutItemDto::hotdealDiscountTotal)
                .sum();

        int afterHotdealProductPrice = productTotalPrice - hotdealDiscount;

        if (afterHotdealProductPrice < 0) {
            afterHotdealProductPrice = 0;
        }

        int deliveryFee = calculateDeliveryFee(memberSeq, items);

        CouponDto selectedCoupon =
                ordersQueryRepository.findAvailableCouponByMemberSeqAndMemberCouponSeq(
                        memberSeq,
                        memberCouponSeq
                );

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
    
    private int calculateDeliveryFee(Long memberSeq, List<CheckoutItemDto> items) {
        boolean usableMembership = ordersQueryRepository.existsUsableMembership(memberSeq);

        if (usableMembership) {
            return 0;
        }

        List<Long> optionsSeqList = items.stream()
                .map(CheckoutItemDto::optionsSeq)
                .filter(optionsSeq -> optionsSeq != null)
                .distinct()
                .toList();

        if (optionsSeqList.isEmpty()) {
            return 0;
        }

        return ordersQueryRepository.findBaseDeliveryFeeByOptionsSeqList(optionsSeqList);
    }
}