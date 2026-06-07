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
    public String list(Model model) {

        model.addAttribute(
                "payments",
                adminPaymentService.getList()
        );

        return "admin/payment/list";
    }
}