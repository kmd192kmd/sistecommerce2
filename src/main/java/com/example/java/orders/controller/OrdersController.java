package com.example.java.orders.controller;

import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;
import com.example.java.orders.dto.CheckoutRequestDto;
import com.example.java.orders.dto.OrderCreateResultDto;
import com.example.java.orders.service.OrdersCommandService;
import com.example.java.orders.service.OrdersViewService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;


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


    /**
     * 주문/결제 화면
     *
     * 현재는 상품/쿠폰/배송지 조회 일부가 테스트 데이터 기준이다.
     * 다만 로그인 여부와 현재 로그인 회원 정보는 실제 로그인 사용자 기준으로 확인한다.
     */
    @GetMapping("/order/checkout")
    public String checkout(@RequestParam(value = "couponSeq", required = false) Long couponSeq,
                           @RequestParam(value = "createdOrderUid", required = false) String createdOrderUid,
                           Authentication authentication,
                           Model model) {

        Long memberSeq = getLoginMemberSeq(authentication);

        /*
            현재 getDeliveryAddresses()는 더미 배송지를 반환한다.
            나중에 회원 배송지 테이블이 완성되면:
            ordersViewService.getDeliveryAddresses(memberSeq)
            형태로 바꾸면 된다.
         */
        model.addAttribute("loginMemberSeq", memberSeq);
        model.addAttribute("cartItems", ordersViewService.getCheckoutItems());
        model.addAttribute("deliveryAddresses", ordersViewService.getDeliveryAddresses());
        model.addAttribute("coupons", ordersViewService.getCoupons());
        model.addAttribute("selectedCouponSeq", couponSeq);
        model.addAttribute("priceSummary", ordersViewService.getPriceSummary(couponSeq));
        model.addAttribute("order", ordersViewService.getOrderPreview());
        model.addAttribute("createdOrderUid", createdOrderUid);

        model.addAttribute("tossClientKey", tossClientKey);
        model.addAttribute("tossSuccessUrl", tossSuccessUrl);
        model.addAttribute("tossFailUrl", tossFailUrl);

        return "order/checkout";
    }

    /**
     * 결제하기 버튼 클릭 시 주문 데이터 생성.
     *
     * 현재 로그인한 회원의 member.seq를 조회해서 orders.member_seq에 저장한다.
     */
    @PostMapping("/order/checkout")
    @ResponseBody
    public ResponseEntity<OrderCreateResultDto> prepareCheckout(CheckoutRequestDto requestDto,
                                                                Authentication authentication) {

        Long memberSeq = getLoginMemberSeq(authentication);

        OrderCreateResultDto result =
                ordersCommandService.createOrderFromCheckout(requestDto, memberSeq);

        return ResponseEntity.ok(result);
    }

    /**
     * 현재 로그인한 회원의 member.seq를 가져온다.
     *
     * 현재 프로젝트의 MemberSecurityService는 Spring Security 기본 User 객체를 반환한다.
     * 따라서 principal에서 memberSeq를 직접 꺼낼 수 없고,
     * authentication.getName()으로 username을 얻은 뒤 member 테이블을 다시 조회해야 한다.
     */
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