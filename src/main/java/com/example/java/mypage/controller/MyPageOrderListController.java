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
            model.addAttribute("orderKeyword", keyword);
            model.addAttribute("period", period);
            
        } catch (Exception e) {
            log.error("주문 목록 조회 중 에러 발생: ", e);
        }

        return "mypage/orders";
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
    
    /**
     * 주문 전체 취소 API
     * 
     * [존재하는 이유]
     * 1. 전액 환불 프로세스 처리: 사용자가 주문 전체(100% 금액)를 한 번에 일괄 취소하려 할 때,
     *    클라이언트 측에서 별도의 상품 ID 목록 파라미터를 넘겨주지 않고도 간단하고 명확하게 동작하도록 함.
     * 2. PG사 연동 최적화: PG사 결제 취소 연동 시 부분 취소(Partial Cancel)가 아닌
     *    원거래 자체에 대한 "전액 취소(Full Cancel)" 요청을 바로 보낼 수 있어 로직이 간결하고 에러 발생 확률이 낮음.
     * 3. 확장성: 주문 상세 화면 등에서 개별 품목 선택 없이 전체를 통째로 취소하는 UI/UX를 대응하기 위해 보존함.
     * 
     * (※ 현재 orders.html 묶음 배송 단위 취소 화면에서는 부분 취소 API인 /cancel-items를 사용하고 있으나,
     *     이 전체 취소 API는 시스템의 완결성과 향후 전체 취소 요구사항을 위해 유지됨.)
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
