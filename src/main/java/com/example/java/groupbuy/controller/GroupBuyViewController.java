package com.example.java.groupbuy.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.java.groupbuy.service.GroupBuyService;
import com.example.java.member.security.CustomUserDetails;
import com.example.java.member.service.MembershipService;

import lombok.RequiredArgsConstructor;

/**
 * 공동구매 페이지(뷰) 라우팅. 얇은 @Controller가 shell 템플릿을 반환한다 (프론트 방식 A).
 * 목록은 Thymeleaf 서버렌더 — 데이터는 GroupBuyService에서 직접 모델에 담는다.
 * (기존 StorefrontController의 임시 데모 라우팅 SampleProducts.groupBuys()를 대체)
 */
@Controller
@RequiredArgsConstructor
public class GroupBuyViewController {

    private final GroupBuyService groupBuyService;
    private final MembershipService membershipService;

    // 토스 결제창에 필요한 값들 (일반 결제 OrdersController와 동일한 설정을 재사용).
    @Value("${toss.client-key}")
    private String tossClientKey;
    @Value("${toss.success-url}")
    private String tossSuccessUrl;
    @Value("${toss.fail-url}")
    private String tossFailUrl;

    /** 공구 목록 페이지 (GB-01) — 진행 중(ONGOING) 공구. */
    @GetMapping("/group-buys")
    public String list(Model model) {
        model.addAttribute("groupBuys", groupBuyService.getSummaries());
        return "groupbuy/list";
    }

    /**
     * 공구 예정 페이지 — 시작 전(SCHEDULED) 공구 목록.
     * 멤버십(활성 구독) 회원은 7일 더 일찍(시작 14일 전부터) 볼 수 있다. 
     * 비로그인은 일반 기준(7일 전).
     */
    @GetMapping("/group-buys/scheduled")
    public String scheduled(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        boolean isMember = user != null && membershipService.isActiveMember(user.getMemberSeq());
        model.addAttribute("groupBuys", groupBuyService.getScheduledSummaries(isMember));
        return "groupbuy/scheduled";
    }

    /**
     * 공구 상세 페이지 (GB-02).
     * shell(detail.html)이 ${groupBuy}로 쓰는 데이터를 모델에 담아준다.
     * (실시간 구매 패널은 추후 React가 맡고, 지금은 같은 데이터로 서버렌더 fallback이 표시)
     */
    @GetMapping("/group-buys/{seq}")
    public String detail(@PathVariable("seq") Long seq,
                         @AuthenticationPrincipal CustomUserDetails user,
                         Model model) {
        model.addAttribute("groupBuy", groupBuyService.getDetail(seq));
        // 토스 결제창 설정(참여 → 결제) — React가 data 속성으로 읽는다. 
        // customerKey는 화면에서 "member-"+seq로 조립.
        model.addAttribute("tossClientKey", tossClientKey);
        model.addAttribute("tossSuccessUrl", tossSuccessUrl);
        model.addAttribute("tossFailUrl", tossFailUrl);
        model.addAttribute("loginMemberSeq", user != null ? user.getMemberSeq() : null);
        return "groupbuy/detail";
    }
}
