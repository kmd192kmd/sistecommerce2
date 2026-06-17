package com.example.java.member.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.member.dto.MemberDto;
import com.example.java.member.service.EmailVerificationService;
import com.example.java.member.service.MemberSignupService;
import com.example.java.member.service.MemberValidationService;
import com.example.java.storefront.SampleProducts;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberSignupService memberSignupService;
    private final EmailVerificationService emailVerificationService;
    private final MemberValidationService memberValidationService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            if ("suspended".equals(error)) {
                model.addAttribute("error", "정지된 계정입니다. 고객센터(admin@shop.com)로 문의해 주세요.");
            } else {
                model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            }
        }
        return "member/login";
    }

    
    // 마이페이지 메인 = 주문목록 (MYP-MAIN-01 / MEM-ORD-01)
    @GetMapping("")
    public String mypageOrders(Model model) {
        model.addAttribute("orders", SampleProducts.orders());
        return "mypage/orders";
    }
    
    @GetMapping("/signup/type")
    public String signupType() {
    	return "member/signup-type";
    }
    
    @GetMapping("/signup/terms")
    public String signupTerms() {
        return "member/signup-terms";
    }

    @PostMapping("/signup/terms")
    public String signupTermsOk(
            @RequestParam(name = "marketing", defaultValue = "false") boolean marketing,
            HttpSession session) {
        session.setAttribute("marketing", marketing);
        return "redirect:/member/signup";
    }
    
    @GetMapping("/signup")
    public String signup() {
        return "member/signup";
    }
    
    @PostMapping("/signup")
    public String signupOk(@ModelAttribute MemberDto memberDto, HttpSession session, Model model) {
        // 이메일 인증 강제: 세션 인증완료 && 인증이메일==가입이메일
        if (!emailVerificationService.isVerified(memberDto.getEmail(), session)) {
            model.addAttribute("error", "이메일 인증을 완료해 주세요.");
            return "member/signup";
        }
        // 중복 재확인(인증~제출 사이 레이스 대비)
        if (memberValidationService.existsByEmail(memberDto.getEmail())) {
            model.addAttribute("error", "이미 가입된 이메일입니다.");
            return "member/signup";
        }

        memberDto.setMarketing((Boolean) session.getAttribute("marketing"));
        memberDto.setEmailVerified("Y");

        boolean success = memberSignupService.signup(memberDto);

        if (!success) {
            model.addAttribute("error", "회원가입에 실패했습니다.");
            return "member/signup";  // 다시 폼으로
        }

        emailVerificationService.clear(session); // 가입 완료 → 인증 세션 정리

        model.addAttribute("signupSuccess", true);
        return "member/signup";  // 성공 시 모달 띄우고 로그인 페이지로
    }
    
    
    
    
}