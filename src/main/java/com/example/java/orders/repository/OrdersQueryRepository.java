package com.example.java.orders.repository;

import com.example.java.orders.dto.CheckoutItemDto;
import com.example.java.orders.dto.CouponDto;

import java.util.List;

public interface OrdersQueryRepository {

    List<CheckoutItemDto> findCheckoutItemsByTestOptionsSeq();

    List<CouponDto> findTestCoupons();

    CouponDto findTestCoupon(Long couponSeq);
}