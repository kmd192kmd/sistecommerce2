package com.example.java.mypage.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.mypage.dto.MyPageOrderListDto;
import com.example.java.mypage.service.MyPageOrderListService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageOrderListController {

    private final MyPageOrderListService myPageOrderListService;

    @GetMapping({"", "/"})
    public String mypageRoot() {
        return "redirect:/mypage/orders";
    }

    /**
     * 마이페이지 - 주문목록 및 배송조회
     */
    @GetMapping("/orders")
    public String getOrders(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "period", required = false) String period,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Long memberSeq = userDetails.getMemberSeq();

        try {
            if (period == null || period.trim().isEmpty()) {
                period = "6months";
            } else if (!"6months".equalsIgnoreCase(period)) {
                try {
                    Integer.parseInt(period);
                } catch (NumberFormatException e) {
                    period = "6months";
                }
            }
            List<MyPageOrderListDto> orders = myPageOrderListService.getOrders(memberSeq, keyword, period);
            model.addAttribute("orders", orders);
            model.addAttribute("keyword", keyword);
            model.addAttribute("period", period);
            
        } catch (Exception e) {
            log.error("주문 목록 조회 중 에러 발생: ", e);
        }

        return "mypage/orderList/orders";
    }
}
