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
     */
    @PostMapping("/{reviewSeq}/edit")
    public String editReview(
            @PathVariable("reviewSeq") Long reviewSeq,
            @RequestParam("productSeq") Long productSeq,
            @RequestParam("rating") Integer rating,
            @RequestParam("content") String content,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            myReviewService.updateReview(reviewSeq, productSeq, userDetails.getMemberSeq(), rating, content);
            redirectAttributes.addFlashAttribute("successMsg", "리뷰가 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            log.error("리뷰 수정 오류", e);
            redirectAttributes.addFlashAttribute("errorMsg", "수정 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage/reviews";
    }

    /**
     * 리뷰 삭제
     */
    @PostMapping("/{reviewSeq}/delete")
    public String deleteReview(
            @PathVariable("reviewSeq") Long reviewSeq,
            @RequestParam("productSeq") Long productSeq,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            myReviewService.deleteReview(reviewSeq, productSeq, userDetails.getMemberSeq());
            redirectAttributes.addFlashAttribute("successMsg", "리뷰가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            log.error("리뷰 삭제 오류", e);
            redirectAttributes.addFlashAttribute("errorMsg", "삭제 중 오류가 발생했습니다.");
        }

        return "redirect:/mypage/reviews";
    }
}
