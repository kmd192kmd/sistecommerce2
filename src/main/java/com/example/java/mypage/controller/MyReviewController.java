package com.example.java.mypage.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.java.member.security.CustomUserDetails;
import com.example.java.mypage.service.MyReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/mypage/reviews")
@RequiredArgsConstructor
public class MyReviewController {

    private final MyReviewService myReviewService;

    /**
     * 내가 쓴 리뷰 목록
     */
    @GetMapping
    public String getMyReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        model.addAttribute("reviews", myReviewService.getMyReviews(userDetails.getMemberSeq()));
        return "mypage/reviews";
    }

    /**
     * 리뷰 수정
     * TODO: 구현 예정
     */
    @PostMapping("/{reviewSeq}/edit")
    public String editReview(
            @PathVariable Long reviewSeq,
            @RequestParam Long productSeq,
            @RequestParam Integer rating,
            @RequestParam String content,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        // TODO: 구현 예정
        return "redirect:/mypage/reviews";
    }

    /**
     * 리뷰 삭제
     * TODO: 구현 예정
     */
    @PostMapping("/{reviewSeq}/delete")
    public String deleteReview(
            @PathVariable Long reviewSeq,
            @RequestParam Long productSeq,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        // TODO: 구현 예정
        return "redirect:/mypage/reviews";
    }
}
