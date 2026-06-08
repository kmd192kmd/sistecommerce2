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

        return CartDto.builder()
                .seq(cart.getSeq())
                .optionsSeq(options.getSeq())
                .quantity(cart.getQuantity())
                .createdDate(cart.getCreatedDate())
                .productSeq(product.getSeq())
                .productName(product.getProductName())
                .price(product.getPrice())
                .thumbnailUrl(product.getThumbnailUrl() != null ? product.getThumbnailUrl() : "/src/images/product/default.png")
                .optionName(options.getDisplayName())
                .additionalPrice(options.getAdditionalPrice())
                .stock(options.getStock())
                .build();
    }

    @Transactional
    public void addCart(CartDto cartDto) {

        Member member = memberRepository.getReferenceById(cartDto.getMemberSeq());
        Options options = optionsRepository.getReferenceById(cartDto.getOptionsSeq());

        Cart cart = Cart.builder()
                .member(member)
                .options(options)
                .quantity(cartDto.getQuantity())
                .createdDate(LocalDate.now())
                .build();

        cartRepository.save(cart);

    }
}
