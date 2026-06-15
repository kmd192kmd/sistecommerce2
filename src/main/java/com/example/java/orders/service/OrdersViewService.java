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

    /*
        바로구매 결제 화면에 표시할 상품 1개를 조회합니다.

        장바구니를 거치지 않고 상품 상세 화면에서 선택한
        optionsSeq와 quantity를 기준으로 결제 상품 정보를 만듭니다.

        기존 장바구니 결제 흐름은 getCheckoutItems()를 그대로 사용하고,
        바로구매 흐름에서만 이 메서드를 사용합니다.
    */
    public CheckoutItemDto getDirectCheckoutItem(Long optionsSeq,
                                                 Integer quantity) {

        CheckoutItemDto item =
                ordersQueryRepository.findDirectCheckoutItem(optionsSeq, quantity);

        if (item == null) {
            throw new IllegalStateException("결제할 수 없는 상품입니다.");
        }

        return item;
    }

    /*
        바로구매 결제 화면의 금액 요약 정보를 계산합니다.

        장바구니 결제는 기존 getPriceSummary()를 그대로 사용하고,
        바로구매 결제는 optionsSeq와 quantity 기준으로
        상품금액, 핫딜 할인, 배송비, 쿠폰 할인, 최종 결제금액을 계산합니다.
    */
    public PriceSummaryDto getDirectPriceSummary(Long memberSeq,
                                                 Long memberCouponSeq,
                                                 Long optionsSeq,
                                                 Integer quantity) {

        CheckoutItemDto item = getDirectCheckoutItem(optionsSeq, quantity);

        /*
            상품 원가 총합.
            핫딜 적용 전 금액이다.
         */
        int productTotalPrice = item.originalTotalPrice();

        /*
            핫딜 할인 총액.
         */
        int hotdealDiscount = item.hotdealDiscountTotal();

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