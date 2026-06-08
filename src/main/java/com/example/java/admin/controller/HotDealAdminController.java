package com.example.java.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.admin.dto.HotDealRequestDto;
import com.example.java.admin.service.HotDealAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class HotDealAdminController {

    private final HotDealAdminService hotDealAdminService;

    /**
     * 관리자: 신규 핫딜 생성 화면(HTML) 렌더링
     */
    @GetMapping("/hotdeal/create")
    public String createForm() {
        
        return "hotdeal/create"; 
    }

    /**
     * 관리자: 폼(Form) 데이터 제출 처리
     */
    @PostMapping("/hotdeal/create")
    public String createHotDeal(@Valid @ModelAttribute HotDealRequestDto requestDto) {
        // @ModelAttribute가 HTML 폼의 name 속성들을 DTO에 찰떡같이 맵핑해줍니다.
        hotDealAdminService.createHotDeal(requestDto);
        
        
        return "redirect:/hotdeal/list"; 
    }
}