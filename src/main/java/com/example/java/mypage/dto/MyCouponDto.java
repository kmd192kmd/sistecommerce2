package com.example.java.mypage.dto;

import java.time.LocalDate;

import com.example.java.member.entity.MemberCoupon;

import lombok.Getter;

@Getter
public class MyCouponDto {

    private final Long memberCouponSeq;
    private final String couponName;
    private final Integer discountType;   // 1=금액할인, 2=비율할인
    private final Integer discountPrice;
    private final Integer discountRate;
    private final LocalDate expireDate;
    private final Integer status;          // 0=미사용, 1=사용완료

    public MyCouponDto(MemberCoupon mc) {
        this.memberCouponSeq = mc.getSeq();
        this.couponName      = mc.getCoupon().getName();
        this.discountType    = mc.getCoupon().getDiscountType();
        this.discountPrice   = mc.getCoupon().getDiscountPrice();
        this.discountRate    = mc.getCoupon().getDiscountRate();
        this.expireDate      = mc.getCoupon().getExpireDate();
        this.status          = mc.getStatus();
    }
}
