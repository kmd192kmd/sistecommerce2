package com.example.java.cart.service;

import com.example.java.cart.dto.CartDto;
import com.example.java.cart.entity.Cart;
import com.example.java.cart.repository.CartLogRepository;
import com.example.java.cart.repository.CartRepository;
import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;
import com.example.java.product.entity.Options;
import com.example.java.product.entity.Product;
import com.example.java.product.repository.OptionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {
    private final CartRepository cartRepository;
    private final CartLogRepository cartLogRepository;
    private final MemberRepository memberRepository;
    private final OptionsRepository optionsRepository;

    public List<CartDto> list(Long memberSeq) {
        return cartRepository.findByMember_Seq(memberSeq).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private CartDto toDto(Cart cart) {
        Options options = cart.getOptions();
        Product product = options.getProduct();

        /*
	        options.getDisplayName() 결과가 비어 있으면
	        장바구니 화면에서 옵션명이 공백으로 보일 수 있습니다.
	
	        옵션을 선택하지 않은 기본 상품은 options 테이블에 기본 옵션처럼 저장되므로,
	        화면에는 "기본 옵션"으로 표시합니다.
	    */
	    String optionName = options.getDisplayName();
	
	    if (optionName == null || optionName.isBlank()) {
	        optionName = "기본 옵션";
	    }
	
	    return CartDto.builder()
	            .seq(cart.getSeq())
	            .optionsSeq(options.getSeq())
	            .quantity(cart.getQuantity())
	            .createdDate(cart.getCreatedDate())
	            .productSeq(product.getSeq())
	            .productName(product.getProductName())
	            .price(product.getPrice())
	            .thumbnailUrl(product.getThumbnailUrl() != null ? product.getThumbnailUrl() : "/src/images/product/default.png")
	            .optionName(optionName)
	            .additionalPrice(options.getAdditionalPrice())
	            .stock(options.getStock())
	            .build();
    }

    @Transactional
    public void addCart(CartDto cartDto) {
    	
    	 /*
	        장바구니 저장 전 기본 입력값을 검증합니다.
	    */
	    if (cartDto.getMemberSeq() == null) {
	        throw new IllegalArgumentException("로그인이 필요합니다.");
	    }
	
	    if (cartDto.getOptionsSeq() == null) {
	        throw new IllegalArgumentException("옵션을 선택해주세요.");
	    }
	
	    if (cartDto.getQuantity() < 1) {
	        throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
	    }

        Member member = memberRepository.getReferenceById(cartDto.getMemberSeq());
        Options options = optionsRepository.getReferenceById(cartDto.getOptionsSeq());

        /*
            구매 / 장바구니 불가 상품 검사

            product 테이블 기준:
            - sale_status = SOLD_OUT : 품절
            - sale_status = STOPPED  : 판매중지
            - hide_yn = Y            : 숨김 상품
            - status = DELETED       : 삭제 상품

            기존 장바구니 로직은 건드리지 않고,
            장바구니 저장 전에 상품 상태만 검사합니다.
        */
        Product product = options.getProduct();

        if ("SOLD_OUT".equals(product.getSaleStatus())
                || "STOPPED".equals(product.getSaleStatus())
                || "Y".equals(product.getHideYn())
                || "DELETED".equals(product.getStatus())) {
            throw new IllegalArgumentException("구매할 수 없는 상품입니다.");
        }

        /*
	        같은 회원이 같은 옵션을 이미 장바구니에 담은 경우에는
	        새로 INSERT하지 않고 기존 장바구니 수량만 증가시킵니다.
	    */
	    Cart existingCart = cartRepository
	            .findByMember_SeqAndOptions_Seq(
	                    cartDto.getMemberSeq(),
	                    cartDto.getOptionsSeq()
	            )
	            .orElse(null);
	
	    if (existingCart != null) {
	
	        int currentQuantity = existingCart.getQuantity() == null ? 0 : existingCart.getQuantity();
	        int newTotalQuantity = currentQuantity + cartDto.getQuantity();
	
	        /*
	            기존 장바구니 수량 + 새로 담는 수량이 재고보다 많으면 막습니다.
	        */
	        if (options.getStock() < newTotalQuantity) {
	            throw new IllegalArgumentException("장바구니 수량이 재고보다 많습니다.");
	        }
	
	        existingCart.increaseQuantity(cartDto.getQuantity());
	
	        return;
	    }
        
        Cart cart = Cart.builder()
                .member(member)
                .options(options)
                .quantity(cartDto.getQuantity())
                .createdDate(LocalDate.now())
                .build();

        cartRepository.save(cart);

        // TODO 나중에 로그 추가

    }
}