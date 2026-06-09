package com.example.java.mypage.controller;

import com.example.java.member.entity.DeliveryAddress;
import com.example.java.member.security.CustomUserDetails;
import com.example.java.member.service.MemberAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/mypage/address")
@RequiredArgsConstructor
public class MyAddressController {

    private final MemberAddressService memberAddressService;

    @GetMapping(value = "")
    public String myAddress(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {

        List<DeliveryAddress> memberAddressList = memberAddressService.myAddress(userDetails.getMemberSeq());

        model.addAttribute("memberAddressList", memberAddressList);

        System.out.println(memberAddressList);

        return "mypage/address";
    }

}
