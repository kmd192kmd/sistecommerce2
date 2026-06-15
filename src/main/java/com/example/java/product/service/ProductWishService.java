package com.example.java.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.dto.ProductWishResponseDto;
import com.example.java.product.entity.ProductWish;
import com.example.java.product.repository.ProductWishRepository;

import lombok.RequiredArgsConstructor;

/**
 * 상품 찜 Service
 */
@Service
@RequiredArgsConstructor
public class ProductWishService {

    private final ProductWishRepository productWishRepository;

    /**
     * 현재 찜한 상태
     */
    private static final String NORMAL = "NORMAL";

    /**
     * 찜 취소 상태
     */
    private static final String DELETED = "DELETED";

    /**
     * 상품 찜 등록 / 취소
     */
    @Transactional
    public ProductWishResponseDto toggleWish(Long memberSeq, Long productSeq) {

        ProductWish wish = productWishRepository
                .findByMemberSeqAndProductSeq(memberSeq, productSeq)
                .orElse(null);

        /*
            기존 찜 데이터가 없으면 새로 INSERT
        */
        if (wish == null) {
            ProductWish newWish = ProductWish.builder()
                    .memberSeq(memberSeq)
                    .productSeq(productSeq)
                    .status(NORMAL)
                    .build();

            productWishRepository.save(newWish);

            return new ProductWishResponseDto(true, "찜 등록 완료");
        }

        /*
            현재 찜한 상태면 찜 취소
        */
        if (NORMAL.equals(wish.getStatus())) {
            wish.setStatus(DELETED);

            return new ProductWishResponseDto(false, "찜 취소 완료");
        }

        /*
            찜 취소 상태면 다시 찜
        */
        wish.setStatus(NORMAL);

        return new ProductWishResponseDto(true, "찜 등록 완료");
    }

    /**
     * 현재 상품 찜 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isWished(Long memberSeq, Long productSeq) {
        return productWishRepository.existsByMemberSeqAndProductSeqAndStatus(
                memberSeq,
                productSeq,
                NORMAL
        );
    }
}