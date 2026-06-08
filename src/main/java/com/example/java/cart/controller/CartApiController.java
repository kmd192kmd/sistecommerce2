package com.example.java.cart.controller;

import com.example.java.cart.dto.CartDto;
import com.example.java.cart.service.CartService;
import com.example.java.member.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartApiController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<?> addCart(@RequestBody CartDto cartDto
                                    , @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        if(customUserDetails == null) return ResponseEntity.status(401).build();

        System.out.println(cartDto.getMemberSeq());
        System.out.println(cartDto.getOptionsSeq());
        System.out.println(cartDto.getQuantity());

        cartDto.setMemberSeq(customUserDetails.getMemberSeq());
        cartService.addCart(cartDto);
        return ResponseEntity.ok().build();
    }

}
