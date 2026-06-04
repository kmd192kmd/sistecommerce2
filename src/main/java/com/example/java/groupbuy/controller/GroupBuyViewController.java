package com.example.java.groupbuy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.java.groupbuy.service.GroupBuyService;

import lombok.RequiredArgsConstructor;

/**
 * 공동구매 페이지(뷰) 라우팅. 얇은 @Controller가 shell 템플릿을 반환한다 (프론트 방식 A).
 * 목록은 Thymeleaf 서버렌더 — 데이터는 GroupBuyService에서 직접 모델에 담는다.
 * (기존 StorefrontController의 임시 데모 라우팅 SampleProducts.groupBuys()를 대체)
 */
@Controller
@RequiredArgsConstructor
public class GroupBuyViewController {

    private final GroupBuyService groupBuyService;

    /** 공구 목록 페이지 (GB-01). */
    @GetMapping("/group-buys")
    public String list(Model model) {
        model.addAttribute("groupBuys", groupBuyService.getSummaries());
        return "groupbuy/list";
    }
}
