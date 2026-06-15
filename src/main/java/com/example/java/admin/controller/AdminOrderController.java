package com.example.java.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.example.java.admin.dto.AdminOrderDetailDto;
import com.example.java.admin.dto.AdminOrderSummaryDto;
import com.example.java.admin.service.AdminOrderService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping("/list")
    public String orderList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "seq"));
        Page<AdminOrderSummaryDto> orders = adminOrderService.getOrders(keyword, pageRequest);
        
        model.addAttribute("orders", orders);
        model.addAttribute("keyword", keyword);
        
        return "admin/order/list";
    }

    @GetMapping("/{orderUid}")
    public String orderDetail(@PathVariable("orderUid") String orderUid, Model model) {
        AdminOrderDetailDto detailDto = adminOrderService.getOrderDetail(orderUid);
        model.addAttribute("detail", detailDto);
        return "admin/order/detail";
    }

}
