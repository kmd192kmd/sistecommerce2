package com.example.java.orders.controller;

import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;
import com.example.java.orders.dto.CheckoutRequestDto;
import com.example.java.orders.dto.OrderCreateResultDto;
import com.example.java.orders.service.OrdersCommandService;
import com.example.java.orders.service.OrdersViewService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersViewService ordersViewService;
    private final OrdersCommandService ordersCommandService;
    private final MemberRepository memberRepository;

    @Value("${toss.client-key}")
    private String tossClientKey;

    @Value("${toss.success-url}")
    private String tossSuccessUrl;

    @Value("${toss.fail-url}")
    private String tossFailUrl;

    @GetMapping("/order/checkout")
    public String checkout(@RequestParam(value = "memberCouponSeq", required = false) Long memberCouponSeq,
                           Authentication authentication,
                           Model model) {

        Long memberSeq = getLoginMemberSeq(authentication);

        model.addAttribute("loginMemberSeq", memberSeq);

        /*
            로그인 회원의 장바구니 상품을 주문 상품으로 표시한다.
         */
        model.addAttribute("cartItems", ordersViewService.getCheckoutItems(memberSeq));

        model.addAttribute("deliveryAddresses", ordersViewService.getDeliveryAddresses());
        model.addAttribute("coupons", ordersViewService.getCoupons(memberSeq));
        model.addAttribute("selectedMemberCouponSeq", memberCouponSeq);
        model.addAttribute("priceSummary", ordersViewService.getPriceSummary(memberSeq, memberCouponSeq));
        model.addAttribute("order", ordersViewService.getOrderPreview());

        model.addAttribute("tossClientKey", tossClientKey);
        model.addAttribute("tossSuccessUrl", tossSuccessUrl);
        model.addAttribute("tossFailUrl", tossFailUrl);

        return "order/checkout";
    }

    @PostMapping("/order/checkout")
    @ResponseBody
    public ResponseEntity<OrderCreateResultDto> prepareCheckout(CheckoutRequestDto requestDto,
                                                                Authentication authentication) {

        Long memberSeq = getLoginMemberSeq(authentication);

        OrderCreateResultDto result =
                ordersCommandService.createOrderFromCheckout(requestDto, memberSeq);

        return ResponseEntity.ok(result);
    }

    private Long getLoginMemberSeq(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        String username = authentication.getName();

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("로그인 회원 정보를 찾을 수 없습니다. username=" + username));

        return member.getSeq();
    }
}