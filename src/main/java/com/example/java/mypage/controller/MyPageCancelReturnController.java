package com.example.java.mypage.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.mypage.dto.MyPageCancelReturnDto;
import com.example.java.mypage.service.MyPageOrderListService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageCancelReturnController {

    private final MyPageOrderListService myPageOrderListService;

    /**
     * 마이페이지 - 취소/반품/교환/환불내역
     */
    @GetMapping("/returns")
    public String getCancelReturns(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Long memberSeq = userDetails.getMemberSeq();

        try {
            List<MyPageCancelReturnDto> cancelReturns = myPageOrderListService.getCancelReturns(memberSeq);
            model.addAttribute("cancelReturns", cancelReturns);
        } catch (Exception e) {
            log.error("취소/반품 내역 조회 중 에러 발생: ", e);
        }

        return "mypage/returns";
    }
}
