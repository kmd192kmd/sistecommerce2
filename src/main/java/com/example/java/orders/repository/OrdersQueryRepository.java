package com.example.java.orders.repository;

import com.example.java.orders.dto.CheckoutItemDto;
import com.example.java.orders.dto.CouponDto;

import java.util.List;

public interface OrdersQueryRepository {

    /**
     * 로그인 회원의 장바구니 중 선택한 cartSeq 목록만 주문/결제 화면용 DTO로 조회한다.
     */
    List<CheckoutItemDto> findCheckoutItemsByMemberCart(Long memberSeq, List<Long> cartSeqList);

    /**
     * 로그인 회원이 발급받은 사용 가능한 쿠폰 목록 조회.
     */
    List<CouponDto> findAvailableCouponsByMemberSeq(Long memberSeq);

    /**
     * 선택한 member_coupon.seq가 실제 로그인 회원의 사용 가능한 쿠폰인지 검증 후 조회.
     */
    CouponDto findAvailableCouponByMemberSeqAndMemberCouponSeq(Long memberSeq, Long memberCouponSeq);
    
    
    
    CheckoutItemDto findDirectCheckoutItem(
            Long optionsSeq,
            Integer quantity);
    
}