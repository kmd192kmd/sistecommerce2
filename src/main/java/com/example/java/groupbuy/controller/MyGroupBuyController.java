package com.example.java.groupbuy.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.groupbuy.dto.MyGroupBuyDto;
import com.example.java.groupbuy.service.MyGroupBuyService;
import com.example.java.member.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/mypage/groupbuy")
@RequiredArgsConstructor
public class MyGroupBuyController {

    private final MyGroupBuyService myGroupBuyService;

    @GetMapping
    public String myGroupBuy(@AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model) {

        if (userDetails == null) {
            return "redirect:/member/login";
        }

        List<MyGroupBuyDto> list = myGroupBuyService.findByMember(userDetails.getMemberSeq());
        model.addAttribute("groupBuyList", list);

        return "mypage/groupbuy";
    }
}
