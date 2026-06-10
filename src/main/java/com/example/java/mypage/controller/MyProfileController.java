package com.example.java.mypage.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.member.service.MyProfileService;
import com.example.java.mypage.dto.MyProfileUpdateDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyProfileController {

    private final MyProfileService myProfileService;

    @GetMapping("/profile")
    public String getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        model.addAttribute("member", myProfileService.getMemberInfo(userDetails.getMemberSeq()));
        return "mypage/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute MyProfileUpdateDto dto,
            RedirectAttributes redirectAttributes) {

        try {
            myProfileService.updateProfile(userDetails.getMemberSeq(), dto);
            redirectAttributes.addFlashAttribute("successMsg", "개인정보가 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            log.error("개인정보 수정 오류", e);
            redirectAttributes.addFlashAttribute("errorMsg", "수정 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage/profile";
    }
}
