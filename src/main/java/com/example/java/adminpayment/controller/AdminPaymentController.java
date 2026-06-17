package com.example.java.adminpayment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.adminpayment.service.AdminPaymentService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @GetMapping("/payments")
    public String list(@org.springframework.web.bind.annotation.RequestParam(value = "keyword", required = false) String keyword, Model model) {

        model.addAttribute("keyword", keyword);
        model.addAttribute("payments", adminPaymentService.getList(keyword));

        return "admin/payment/list";
    }

    @org.springframework.web.bind.annotation.PostMapping("/payments/{seq}/status")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<String> changeStatus(@org.springframework.web.bind.annotation.PathVariable("seq") Long seq, @org.springframework.web.bind.annotation.RequestParam(value="status") Integer status) {
        adminPaymentService.changeStatus(seq, status);
        return org.springframework.http.ResponseEntity.ok("상태가 변경되었습니다.");
    }
}