package com.example.java.mypage.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.mypage.dto.MyPageOrderListDto;
import com.example.java.mypage.dto.MyPageCancelReturnDto;
import com.example.java.mypage.service.MyPageOrderListService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageOrderApiController {

    private final MyPageOrderListService myPageOrderListService;

    /**
     * 마이페이지 - 주문목록 조회 API
     */
    @GetMapping("/orders")
    public List<MyPageOrderListDto> getOrders(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "period", required = false) String period,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long memberSeq = userDetails.getMemberSeq();
        
        if (period == null || period.trim().isEmpty()) {
            period = "6months";
        }
        
        return myPageOrderListService.getOrders(memberSeq, keyword, period);
    }

    /**
     * 마이페이지 - 취소/반품/교환/환불내역 조회 API
     */
    @GetMapping("/returns")
    public List<MyPageCancelReturnDto> getCancelReturns(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long memberSeq = userDetails.getMemberSeq();
        return myPageOrderListService.getCancelReturns(memberSeq);
    }
}
