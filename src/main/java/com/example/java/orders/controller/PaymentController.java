package com.example.java.orders.controller;

import com.example.java.orders.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/payments/success")
    public String success(@RequestParam("paymentKey") String paymentKey,
                          @RequestParam("orderId") String orderId,
                          @RequestParam("amount") Integer amount,
                          Model model) {

        paymentService.confirmPayment(paymentKey, orderId, amount);

        model.addAttribute("orderId", orderId);
        model.addAttribute("amount", amount);

        return "order/payment-success";
    }

    @GetMapping("/payments/fail")
    public String fail(@RequestParam(value = "code", required = false) String code,
                       @RequestParam(value = "message", required = false) String message,
                       @RequestParam(value = "orderId", required = false) String orderId,
                       Model model) {

        paymentService.markPaymentFail(orderId, code, message);

        model.addAttribute("code", code);
        model.addAttribute("message", message);
        model.addAttribute("orderId", orderId);

        return "order/payment-fail";
    }
}