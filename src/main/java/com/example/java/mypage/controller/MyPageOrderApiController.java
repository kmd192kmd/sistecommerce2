package com.example.java.mypage.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.mypage.dto.MyPageOrderListDto;
import com.example.java.mypage.dto.MyPageCancelReturnDto;
import com.example.java.mypage.service.MyPageOrderListService;
import com.example.java.orders.service.PaymentService;
import com.example.java.orders.dto.OrderItemCancelRequestDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageOrderApiController {

    private final MyPageOrderListService myPageOrderListService;
    private final PaymentService paymentService;

    /**
     * 마이페이지 - 주문목록 조회 API
     */
    @GetMapping("/orders")
    public List<MyPageOrderListDto> getOrders(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "offset", required = false) Long offset,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long memberSeq = userDetails.getMemberSeq();
        
        if (period == null || period.trim().isEmpty()) {
            period = "6months";
        }
        
        long queryOffset = (offset != null) ? offset : (long) page * size;
        return myPageOrderListService.getOrders(memberSeq, keyword, period, queryOffset, size);
    }

    /**
     * 마이페이지 - 취소/반품/교환/환불내역 조회 API
     */
    @GetMapping("/returns")
    public List<MyPageCancelReturnDto> getCancelReturns(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "offset", required = false) Long offset,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long memberSeq = userDetails.getMemberSeq();
        long queryOffset = (offset != null) ? offset : (long) page * size;
        return myPageOrderListService.getCancelReturns(memberSeq, queryOffset, size);
    }

    /**
     * 주문 전체 취소 API
     */
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
    
    /**
     * 주문 부분 품목 취소 API
     */
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

    /**
     * 주문 부분 품목 반품 신청 API
     */
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
