package com.example.java.mypage.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.mypage.dto.MyPageOrderListDto;
import com.example.java.mypage.dto.MyPageCancelReturnDto;
import com.example.java.mypage.dto.MyPageOrderDetailDto;
import com.example.java.mypage.service.MyPageOrderListService;
import com.example.java.orders.service.PaymentService;
import com.example.java.orders.dto.OrderItemCancelRequestDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageOrderListController {

    private final MyPageOrderListService myPageOrderListService;
    private final PaymentService paymentService;
    
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

        return "mypage/orders";
    }

    /**
     * 마이페이지 - 취소/반품/교환/환불내역
     */
    @GetMapping("/returns")
    public String getCancelReturns(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Long memberSeq = userDetails.getMemberSeq();

        try {
            List<MyPageCancelReturnDto> cancelReturns = myPageOrderListService.getCancelReturns(memberSeq);
            model.addAttribute("cancelReturns", cancelReturns);
        } catch (Exception e) {
            log.error("취소/반품 내역 조회 중 에러 발생: ", e);
        }

        return "mypage/returns";
    }

    /**
     * 마이페이지 - 주문 상세 조회
     */
    @GetMapping("/orders/{orderSeq}")
    public String getOrderDetail(
            @PathVariable("orderSeq") Long orderSeq,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        try {
            MyPageOrderDetailDto orderDetail = myPageOrderListService.getOrderDetail(orderSeq);
            if (orderDetail == null) {
                return "redirect:/mypage/orders";
            }
            model.addAttribute("orderDetail", orderDetail);
        } catch (Exception e) {
            log.error("주문 상세 조회 중 에러 발생: ", e);
            return "redirect:/mypage/orders";
        }

        return "mypage/orderDetail";
    }
    
    @PostMapping("/orders/{orderSeq}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable("orderSeq") Long orderSeq,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberSeq = userDetails.getMemberSeq();

        paymentService.cancelPayment(orderSeq, memberSeq);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "주문이 취소되었습니다."
        ));
    }
    
    @PostMapping("/orders/{orderSeq}/cancel-items")
    public ResponseEntity<Map<String, Object>> cancelOrderItems(
            @PathVariable("orderSeq") Long orderSeq,
            @RequestBody OrderItemCancelRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            Long memberSeq = userDetails.getMemberSeq();

            paymentService.cancelOrderItems(
                    orderSeq,
                    memberSeq,
                    requestDto.getOrderItemSeqList(),
                    requestDto.getCancelReason()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "선택한 상품의 주문이 취소되었습니다."
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/orders/{orderSeq}/return")
    public ResponseEntity<Map<String, Object>> returnOrderItems(
            @PathVariable("orderSeq") Long orderSeq,
            @RequestBody OrderItemCancelRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            Long memberSeq = userDetails.getMemberSeq();

            paymentService.requestReturnItems(
                    orderSeq,
                    memberSeq,
                    requestDto.getOrderItemSeqList(),
                    requestDto.getCancelReason()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "반품 신청이 접수되었습니다."
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
