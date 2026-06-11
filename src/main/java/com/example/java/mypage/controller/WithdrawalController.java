package com.example.java.mypage.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.member.service.WithdrawalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 마이페이지 회원 탈퇴 신청·복구 컨트롤러.
 */
@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @GetMapping("/withdrawal")
    public String page(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("view", withdrawalService.view(userDetails.getMemberSeq()));
        return "mypage/withdrawal";
    }

    @PostMapping("/withdrawal")
    public String request(@AuthenticationPrincipal CustomUserDetails userDetails,
                          @RequestParam(name = "reasonSeq") Long reasonSeq,
                          RedirectAttributes redirectAttributes) {
        try {
            withdrawalService.request(userDetails.getMemberSeq(), reasonSeq);
            redirectAttributes.addFlashAttribute("successMsg",
                    "탈퇴 신청이 접수되었습니다. 3일 이내에 복구할 수 있습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            log.error("탈퇴 신청 오류", e);
            redirectAttributes.addFlashAttribute("errorMsg", "탈퇴 신청 중 오류가 발생했습니다.");
        }
        return "redirect:/mypage/withdrawal";
    }

    @PostMapping("/withdrawal/restore")
    public String restore(@AuthenticationPrincipal CustomUserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        try {
            withdrawalService.restore(userDetails.getMemberSeq());
            redirectAttributes.addFlashAttribute("successMsg", "탈퇴 신청이 취소되었습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            log.error("탈퇴 복구 오류", e);
            redirectAttributes.addFlashAttribute("errorMsg", "복구 중 오류가 발생했습니다.");
        }
        return "redirect:/mypage/withdrawal";
    }
}
