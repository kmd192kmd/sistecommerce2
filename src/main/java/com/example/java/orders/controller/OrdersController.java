package com.example.java.orders.controller;

import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;
import com.example.java.orders.dto.CheckoutRequestDto;
import com.example.java.orders.dto.OrderCreateResultDto;
import com.example.java.orders.service.OrdersCommandService;
import com.example.java.orders.service.OrdersViewService;
import com.example.java.orders.dto.CheckoutItemDto;

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

import java.util.List;

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
    public String checkout(@RequestParam(value = "cartSeq", required = false) List<Long> cartSeqList,
                           @RequestParam(value = "memberCouponSeq", required = false) Long memberCouponSeq,
                           @RequestParam(value = "directBuy", required = false, defaultValue = "false") Boolean directBuy,
                           @RequestParam(value = "optionsSeq", required = false) Long optionsSeq,
                           @RequestParam(value = "quantity", required = false) Integer quantity,
                           Authentication authentication,
                           Model model) {

        Long memberSeq = getLoginMemberSeq(authentication);

        boolean isDirectBuy = Boolean.TRUE.equals(directBuy);

        model.addAttribute("loginMemberSeq", memberSeq);
        model.addAttribute("deliveryAddresses", ordersViewService.getDeliveryAddresses(memberSeq));
        model.addAttribute("coupons", ordersViewService.getCoupons(memberSeq));
        model.addAttribute("selectedMemberCouponSeq", memberCouponSeq);
        model.addAttribute("order", ordersViewService.getOrderPreview());

        model.addAttribute("tossClientKey", tossClientKey);
        model.addAttribute("tossSuccessUrl", tossSuccessUrl);
        model.addAttribute("tossFailUrl", tossFailUrl);

        if (isDirectBuy) {
            if (optionsSeq == null) {
                throw new IllegalArgumentException("바로구매할 상품 옵션을 선택해야 합니다.");
            }

            if (quantity == null || quantity < 1) {
                throw new IllegalArgumentException("구매 수량은 1개 이상이어야 합니다.");
            }

            List<com.example.java.orders.dto.CheckoutItemDto> items =
                    ordersViewService.getDirectCheckoutItems(optionsSeq, quantity);

            model.addAttribute("cartItems", items);
            model.addAttribute("priceSummary",
                    ordersViewService.getPriceSummaryByItems(memberSeq, memberCouponSeq, items));

            model.addAttribute("selectedCartSeqList", List.of());
            model.addAttribute("directBuy", true);
            model.addAttribute("directOptionsSeq", optionsSeq);
            model.addAttribute("directQuantity", quantity);

            return "order/checkout";
        }

        if (cartSeqList == null || cartSeqList.isEmpty()) {
            throw new IllegalArgumentException("결제할 장바구니 상품을 선택해야 합니다.");
        }

        model.addAttribute("cartItems", ordersViewService.getCheckoutItems(memberSeq, cartSeqList));
        model.addAttribute("priceSummary",
                ordersViewService.getPriceSummary(memberSeq, memberCouponSeq, cartSeqList));

        model.addAttribute("selectedCartSeqList", cartSeqList);
        model.addAttribute("directBuy", false);
        model.addAttribute("directOptionsSeq", null);
        model.addAttribute("directQuantity", null);

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