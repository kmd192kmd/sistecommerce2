package com.example.java.cart.controller;

import com.example.java.cart.dto.CartDto;
import com.example.java.cart.service.CartService;
import com.example.java.member.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartApiController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<?> addCart(@RequestBody CartDto cartDto,
                                     @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        /*
            로그인하지 않은 상태에서 장바구니 담기를 누르면
            401 Unauthorized를 반환합니다.

            상품 상세 화면 JavaScript에서 이 응답을 받으면
            로그인 페이지로 이동시킬 수 있습니다.
        */
        if (customUserDetails == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        /*
        	확인용 출력입니다.
        */
        System.out.println(cartDto.getMemberSeq());
        System.out.println(cartDto.getOptionsSeq());
        System.out.println(cartDto.getQuantity());

        /*
            클라이언트에서 memberSeq를 보내더라도 믿지 않고,
            현재 로그인한 사용자 정보에서 memberSeq를 직접 넣습니다.

            그래야 다른 회원 번호로 장바구니를 조작하는 것을 막을 수 있습니다.
        */
        cartDto.setMemberSeq(customUserDetails.getMemberSeq());

        cartService.addCart(cartDto);

        /*
            화면에서 메시지를 읽을 수 있도록 문자열을 반환합니다.
        */
        return ResponseEntity.ok("장바구니에 담겼습니다.");
    }

    /** 수량 변경: PATCH /api/cart/{seq}  body: {"quantity": N} */
    @PatchMapping("/{seq}")
    public ResponseEntity<?> updateQuantity(@PathVariable Long seq,
                                            @RequestBody Map<String, Integer> body,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        Integer quantity = body.get("quantity");
        if (quantity == null) {
            return ResponseEntity.badRequest().body("수량을 입력해주세요.");
        }

        cartService.updateQuantity(seq, userDetails.getMemberSeq(), quantity);
        return ResponseEntity.ok("수량이 변경되었습니다.");
    }

    /** 항목 삭제: DELETE /api/cart/{seq} */
    @DeleteMapping("/{seq}")
    public ResponseEntity<?> removeItem(@PathVariable Long seq,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        cartService.removeItem(seq, userDetails.getMemberSeq());
        return ResponseEntity.ok("삭제되었습니다.");
    }


    /*
        CartService에서 IllegalArgumentException이 발생하면
        400 Bad Request로 응답합니다.

        예:
        - 옵션을 선택하지 않음
        - 수량이 1보다 작음
        - 재고 부족
    */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {

        return ResponseEntity
                .badRequest()
                .body(e.getMessage());
    }


    /*
        예상하지 못한 오류가 발생했을 때 처리합니다.
    */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {

        return ResponseEntity
                .internalServerError()
                .body("장바구니 처리 중 오류가 발생했습니다.");
    }
}