package com.example.java.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.java.admin.service.AdminMemberService;
import com.example.java.member.entity.Member;
import com.example.java.admin.dto.AdminOrderSummaryDto;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class AdminMemberController {
	
    private final AdminMemberService adminMemberService;

    @GetMapping("/admin/member/list")
    public String memberList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        // 최신 가입자가 위로 오도록 내림차순(DESC) 정렬 추가
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "seq"));

        // Member 엔티티 그대로 가져오기
        Page<Member> members = adminMemberService.getMembers(keyword, pageRequest);

        model.addAttribute("members", members);
        model.addAttribute("keyword", keyword);

        return "admin/member/list"; // 이전에 파일명을 list.html 로 변경하셨다고 가정
    }

    @GetMapping("/admin/member/{seq}")
    public String memberDetail(@PathVariable("seq") Long seq, Model model) {
        Member member = adminMemberService.getMember(seq);
        List<AdminOrderSummaryDto> orders = adminMemberService.getMemberOrders(seq);
        model.addAttribute("member", member);
        model.addAttribute("orders", orders);
        return "admin/member/detail";
    }

    @PostMapping("/admin/member/{seq}/status")
    public String updateMemberStatus(@PathVariable("seq") Long seq, @RequestParam("status") Integer status) {
        adminMemberService.updateMemberStatus(seq, status);
        return "redirect:/admin/member/" + seq;
    }
}
