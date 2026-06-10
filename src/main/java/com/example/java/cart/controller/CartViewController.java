package com.example.java.cart.controller;

import com.example.java.cart.dto.CartDto;
import com.example.java.cart.service.CartService;
import com.example.java.member.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartViewController {

    private final CartService cartService;

    @GetMapping(value = "")
    public String cart(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {

    	/*
	        로그인하지 않은 사용자가 장바구니 페이지에 접근하면
	        로그인 페이지로 이동시킵니다.
	    */
	    if (userDetails == null) {
	        return "redirect:/member/login";
	    }
    	
        Long memberSeq = userDetails.getMemberSeq();

        List<CartDto> cartList = cartService.list(memberSeq);

        model.addAttribute("cartList", cartList);

        return "cart/cart";
    }


}
