package com.example.java.groupbuy.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.groupbuy.dto.GroupBuyDetailResponse;
import com.example.java.groupbuy.dto.GroupBuySummaryResponse;
import com.example.java.groupbuy.service.GroupBuyService;

import lombok.RequiredArgsConstructor;

/**
 * 공동구매 REST API. (Thymeleaf shell 위에 mount된 React가 호출)
 * 회원 노출용 데이터만 제공 — 옵션 잔여 수량 등 비공개 정보는 내려보내지 않는다.
 */
@RestController
@RequestMapping("/api/group-buys")
@RequiredArgsConstructor
public class GroupBuyApiController {

    private final GroupBuyService groupBuyService;

    /** 공구 목록. */
    @GetMapping
    public List<GroupBuySummaryResponse> list() {
        return groupBuyService.getSummaries();
    }

    /** 공구 상세. */
    @GetMapping("/{seq}")
    public GroupBuyDetailResponse detail(@PathVariable Long seq) {
        return groupBuyService.getDetail(seq);
    }
}
