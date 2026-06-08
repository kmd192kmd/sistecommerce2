package com.example.java.admin.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.java.admin.dto.CouponRequestDto;
import com.example.java.admin.dto.MemberSearchDto;
import com.example.java.admin.service.CouponAdminService;
import com.example.java.member.entity.Coupon;
import com.example.java.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class CouponAdminController {	

    // 앞서 만든 Service를 주입받습니다.
    private final CouponAdminService couponAdminService;
    
    
    
    @GetMapping("/coupon/create")
    public String createForm(Model model) {
    	
        model.addAttribute("couponRequestDto", new CouponRequestDto());
        
        return "coupon/create";
    }
    
	@GetMapping("/coupon/list")
	public String coupon(Model model) {
		
		// 1. Service를 통해 DB에서 쿠폰 목록을 전부 가져옵니다.
        List<Coupon> couponList = couponAdminService.getAllCoupons();

        
        // 2. HTML(Thymeleaf)에서 사용할 수 있도록 'coupons'라는 이름으로 모델에 담습니다.
        model.addAttribute("coupons", couponList);
		
		return "coupon/list";
	}
	
		@ResponseBody
	    @GetMapping("/admin/coupons/search-members")
	    public ResponseEntity<List<MemberSearchDto>> searchMembers(@RequestParam("keyword") String keyword) {
        
        // 프론트엔드(JS)에서 넘어온 keyword(예: "홍길동")를 통째로 서비스에 넘깁니다.
        List<MemberSearchDto> searchMember = couponAdminService.searchMembersForCoupon(keyword);
        
        // 검색된 회원 목록을 JSON 형태로 변환해서 응답합니다.
        return ResponseEntity.ok(searchMember);
    }



    /**
     * 1. 쿠폰 생성 API
     * POST /api/admin/coupons
     */
	@PostMapping("/coupon/list") // HTML 폼 action과 일치하도록 경로 재조정
    public String createCoupon(CouponRequestDto dto) {
		
        couponAdminService.createCoupon(dto);
        
        return "redirect:/coupon/list"; // 성공 시 리스트로 이동
    }
	
	@PostMapping("/api/admin/coupons/issue")
    public String issueCouponManually(
            @RequestParam("couponSeq") Long couponSeq,
            @RequestParam("issueType") String issueType,
            @RequestParam(value = "memberSeqs", required = false) String memberSeqs) {

        // 서비스에게 쿠폰 번호, 발급 타입(전체/특정), 회원번호들을 넘겨줍니다.
        couponAdminService.issueCouponManual(couponSeq, issueType, memberSeqs);

        // 처리가 끝나면 다시 쿠폰 리스트 화면으로 돌아갑니다.
        return "redirect:/coupon/list";
    }
	
	@ResponseBody
    @org.springframework.web.bind.annotation.DeleteMapping("/api/admin/coupons/{seq}")
    public org.springframework.http.ResponseEntity<String> deleteCoupon(@org.springframework.web.bind.annotation.
PathVariable("seq") Long seq) {
        couponAdminService.deleteCoupon(seq);
        return org.springframework.http.ResponseEntity.ok("success");
    }
	
	@GetMapping("/admin/coupons/{seq}/edit")
    public String editForm(@PathVariable("seq") Long seq, Model model) {
        Coupon coupon = couponAdminService.getCoupon(seq);
        model.addAttribute("coupon", coupon);
        return "coupon/create"; // <--- 여기를 edit 대신 create로 변경!
    }

    @org.springframework.web.bind.annotation.PutMapping("/admin/coupons/{seq}/edit")
    public String updateCoupon(@PathVariable("seq") Long seq, CouponRequestDto dto) {
        couponAdminService.updateCoupon(seq, dto);
        return "redirect:/coupon/list";
    }
    
    @PostMapping("/admin/coupons/{seq}/edit")
    public String updateCouponPost(@PathVariable("seq") Long seq, CouponRequestDto dto) {
        couponAdminService.updateCoupon(seq, dto);
        return "redirect:/coupon/list";
    }

	
    
    // (이후에 쿠폰 발급, 수정 등의 API가 이 아래에 추가될 예정입니다)
}