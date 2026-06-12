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
     * 상품 상세 화면의 바로구매용 상품 조회.
     */
    CheckoutItemDto findCheckoutItemByOptionsSeq(Long optionsSeq, Integer quantity);

    /**
     * 선택한 member_coupon.seq가 실제 로그인 회원의 사용 가능한 쿠폰인지 검증 후 조회.
     */
    CouponDto findAvailableCouponByMemberSeqAndMemberCouponSeq(Long memberSeq, Long memberCouponSeq);
    
    /**
     * 선택한 옵션 목록의 택배사 기본 배송비 합계 조회.
     * 같은 택배사는 중복 배송비를 제거하고 한 번만 계산한다.
     */
    int findBaseDeliveryFeeByOptionsSeqList(List<Long> optionsSeqList);

    /**
     * 회원이 멤버십 혜택 유지 상태인지 확인.
     * active: 가입 상태
     * canceled: 취소 예정이지만 만료일까지 혜택 유지
     */
    boolean existsUsableMembership(Long memberSeq);
}