package com.example.java.groupbuy.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

    // 결제대기 항목의 "결제하기" 버튼이 띄울 토스 결제창 설정 (공구 상세 GroupBuyViewController와 동일).
    @Value("${toss.client-key}")
    private String tossClientKey;
    @Value("${toss.success-url}")
    private String tossSuccessUrl;
    @Value("${toss.fail-url}")
    private String tossFailUrl;

    @GetMapping
    public String myGroupBuy(@AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model) {

        if (userDetails == null) {
            return "redirect:/member/login";
        }

        List<MyGroupBuyDto> list = myGroupBuyService.findByMember(userDetails.getMemberSeq());
        model.addAttribute("groupBuyList", list);
        // 결제대기 → 토스 결제창 재진입에 필요한 값. customerKey는 화면에서 "member-"+seq로 조립.
        model.addAttribute("tossClientKey", tossClientKey);
        model.addAttribute("tossSuccessUrl", tossSuccessUrl);
        model.addAttribute("tossFailUrl", tossFailUrl);
        model.addAttribute("loginMemberSeq", userDetails.getMemberSeq());

        return "mypage/groupbuy";
    }
}
