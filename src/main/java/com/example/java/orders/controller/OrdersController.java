package com.example.java.orders.controller;

import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;
import com.example.java.member.security.CustomUserDetails;
import com.example.java.orders.dto.CheckoutItemDto;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
    public String checkout(
            @RequestParam(value = "cartSeq", required = false)
            List<Long> cartSeqList,

            @RequestParam(value = "memberCouponSeq", required = false)
            Long memberCouponSeq,

            @RequestParam(
                    value = "directBuy",
                    required = false,
                    defaultValue = "false"
            )
            Boolean directBuy,

            @RequestParam(value = "optionsSeq", required = false)
            Long optionsSeq,

            @RequestParam(value = "quantity", required = false)
            Integer quantity,

            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Long memberSeq = getLoginMemberSeq(authentication);
        boolean isDirectBuy = Boolean.TRUE.equals(directBuy);

        model.addAttribute("loginMemberSeq", memberSeq);
        model.addAttribute(
                "deliveryAddresses",
                ordersViewService.getDeliveryAddresses(memberSeq)
        );
        model.addAttribute(
                "coupons",
                ordersViewService.getCoupons(memberSeq)
        );
        model.addAttribute("selectedMemberCouponSeq", memberCouponSeq);
        model.addAttribute("order", ordersViewService.getOrderPreview());

        model.addAttribute("tossClientKey", tossClientKey);
        model.addAttribute("tossSuccessUrl", tossSuccessUrl);
        model.addAttribute("tossFailUrl", tossFailUrl);

        /*
         * 바로구매 결제 화면 진입
         */
        if (isDirectBuy) {
            if (optionsSeq == null) {
                throw new IllegalArgumentException(
                        "바로구매할 상품 옵션을 선택해야 합니다."
                );
            }

            if (quantity == null || quantity < 1) {
                throw new IllegalArgumentException(
                        "구매 수량은 1개 이상이어야 합니다."
                );
            }

            List<CheckoutItemDto> items =
                    ordersViewService.getDirectCheckoutItems(
                            optionsSeq,
                            quantity
                    );

            model.addAttribute("cartItems", items);

            model.addAttribute(
                    "priceSummary",
                    ordersViewService.getPriceSummaryByItems(
                            memberSeq,
                            memberCouponSeq,
                            items
                    )
            );

            model.addAttribute("selectedCartSeqList", List.of());
            model.addAttribute("directBuy", true);
            model.addAttribute("directOptionsSeq", optionsSeq);
            model.addAttribute("directQuantity", quantity);

            return "order/checkout";
        }

        /*
         * 장바구니 결제 화면 진입
         */
        if (cartSeqList == null || cartSeqList.isEmpty()) {
            throw new IllegalArgumentException(
                    "결제할 장바구니 상품을 선택해야 합니다."
            );
        }

        /*
         * 선택한 장바구니 상품 중 재고가 부족한 상품을 조회한다.
         */
        List<String> insufficientStockProductNames =
                ordersViewService.getInsufficientStockProductNames(
                        memberSeq,
                        cartSeqList
                );

        /*
         * 재고 부족 상품이 하나라도 존재하면 상품명을 알리고
         * 결제 화면 대신 장바구니 화면으로 돌아간다.
         */
        if (!insufficientStockProductNames.isEmpty()) {
            String stockAlertMessage =
                    "다음 상품의 재고가 부족합니다.\n- "
                            + String.join(
                                    "\n- ",
                                    insufficientStockProductNames
                            );

            redirectAttributes.addFlashAttribute(
                    "stockAlertMessage",
                    stockAlertMessage
            );

            return "redirect:/cart";
        }

        List<CheckoutItemDto> items =
                ordersViewService.getCheckoutItems(
                        memberSeq,
                        cartSeqList
                );

        model.addAttribute("cartItems", items);

        model.addAttribute(
                "priceSummary",
                ordersViewService.getPriceSummaryByItems(
                        memberSeq,
                        memberCouponSeq,
                        items
                )
        );

        model.addAttribute("selectedCartSeqList", cartSeqList);
        model.addAttribute("directBuy", false);
        model.addAttribute("directOptionsSeq", null);
        model.addAttribute("directQuantity", null);

        return "order/checkout";
    }

    /*
     * 기존 별도 바로구매 결제 화면
     */
    @GetMapping("/order/checkout/direct")
    public String directCheckout(
            @RequestParam("optionsSeq")
            Long optionsSeq,

            @RequestParam("quantity")
            Integer quantity,

            @RequestParam(value = "memberCouponSeq", required = false)
            Long memberCouponSeq,

            Authentication authentication,
            Model model
    ) {
        Long memberSeq = getLoginMemberSeq(authentication);

        model.addAttribute("loginMemberSeq", memberSeq);

        model.addAttribute(
                "cartItems",
                List.of(
                        ordersViewService.getDirectCheckoutItem(
                                optionsSeq,
                                quantity
                        )
                )
        );

        model.addAttribute(
                "deliveryAddresses",
                ordersViewService.getDeliveryAddresses(memberSeq)
        );

        model.addAttribute(
                "coupons",
                ordersViewService.getCoupons(memberSeq)
        );

        model.addAttribute("selectedMemberCouponSeq", memberCouponSeq);

        model.addAttribute(
                "priceSummary",
                ordersViewService.getDirectPriceSummary(
                        memberSeq,
                        memberCouponSeq,
                        optionsSeq,
                        quantity
                )
        );

        model.addAttribute("order", ordersViewService.getOrderPreview());

        model.addAttribute("tossClientKey", tossClientKey);
        model.addAttribute("tossSuccessUrl", tossSuccessUrl);
        model.addAttribute("tossFailUrl", tossFailUrl);

        model.addAttribute("directBuyYn", true);
        model.addAttribute("directBuy", true);
        model.addAttribute("directOptionsSeq", optionsSeq);
        model.addAttribute("directQuantity", quantity);

        return "order/checkout";
    }

    @PostMapping("/order/checkout")
    @ResponseBody
    public ResponseEntity<OrderCreateResultDto> prepareCheckout(
            CheckoutRequestDto requestDto,
            Authentication authentication
    ) {
        Long memberSeq = getLoginMemberSeq(authentication);

        OrderCreateResultDto result =
                ordersCommandService.createOrderFromCheckout(
                        requestDto,
                        memberSeq
                );

        return ResponseEntity.ok(result);
    }

    private Long getLoginMemberSeq(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getMemberSeq();
        }

        String username = authentication.getName();

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "로그인 회원 정보를 찾을 수 없습니다. username="
                                + username
                ));

        return member.getSeq();
    }
}