package com.example.java.mypage.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.member.service.MemberCouponService;
import com.example.java.mypage.dto.MyCouponDto;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyCouponController {

    private final MemberCouponService memberCouponService;

    @GetMapping("/coupons")
    public String getMyCoupons(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Long memberSeq = userDetails.getMemberSeq();
        List<MyCouponDto> coupons = memberCouponService.getMyCoupons(memberSeq);

        model.addAttribute("coupons", coupons);
        return "mypage/coupons";
    }
}
