package com.example.java.member.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.java.member.entity.Member;
import com.example.java.member.entity.Memberships;
import com.example.java.member.repository.MemberRepository;
import com.example.java.member.security.CustomUserDetails;
import com.example.java.member.service.MembershipService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/mypage/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService  membershipService;
    private final MemberRepository   memberRepository;

    @Value("${toss.client-key}")
    private String tossClientKey;

    // -------------------------------------------------------------------------
    // 멤버십 페이지
    // -------------------------------------------------------------------------

    @GetMapping
    public String membershipPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
        Member member = memberRepository.findById(userDetails.getMemberSeq())
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다."));

        Optional<Memberships> memberships = membershipService.findByMember(member);

        model.addAttribute("membership", memberships.orElse(null));
        model.addAttribute("price", membershipService.getMembershipPrice());
        model.addAttribute("tossClientKey", tossClientKey);
        // customerKey: Toss 위젯에서 사용할 회원 고유 키
        model.addAttribute("customerKey", "member-" + member.getSeq());

        return "mypage/membership";
    }

    // -------------------------------------------------------------------------
    // 가입 — Toss 빌링 인증 콜백 (GET)
    // -------------------------------------------------------------------------

    /**
     * Toss requestBillingAuth() 성공 후 리다이렉트되는 콜백.
     * URL: /mypage/membership/billing/success?authKey=xxx&customerKey=xxx
     */
    @GetMapping("/billing/success")
    public String billingSuccess(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam("authKey") String authKey,
                                 @RequestParam("customerKey") String customerKey,
                                 RedirectAttributes redirectAttributes) {
        try {
            Member member = memberRepository.findById(userDetails.getMemberSeq())
                    .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다."));

            membershipService.join(member, authKey, customerKey);
            redirectAttributes.addFlashAttribute("successMessage", "멤버십 가입이 완료되었습니다. 🎉");

        } catch (IllegalStateException e) {
            log.warn("멤버십 가입 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/mypage/membership";
    }

    /**
     * Toss requestBillingAuth() 실패 콜백.
     */
    @GetMapping("/billing/fail")
    public String billingFail(@RequestParam(value = "message", required = false) String message,
                              RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "카드 등록에 실패했습니다. " + (message != null ? message : ""));
        return "redirect:/mypage/membership";
    }

    // -------------------------------------------------------------------------
    // 취소 예정
    // -------------------------------------------------------------------------

    @PostMapping("/cancel")
    public String cancel(@AuthenticationPrincipal CustomUserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            Member member = memberRepository.findById(userDetails.getMemberSeq())
                    .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다."));

            membershipService.scheduleCancel(member);
            redirectAttributes.addFlashAttribute("successMessage", "멤버십이 취소 예정 상태로 변경되었습니다. 만료일까지 혜택이 유지됩니다.");

        } catch (IllegalStateException e) {
            log.warn("멤버십 취소 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/mypage/membership";
    }
}
