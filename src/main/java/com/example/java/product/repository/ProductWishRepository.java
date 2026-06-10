package com.example.java.product.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.product.entity.ProductWish;

/**
 * 상품 찜 Repository
 *
 * product_wish 테이블 조회/저장/수정 담당
 */
public interface ProductWishRepository extends JpaRepository<ProductWish, Long> {

    /**
     * 특정 회원이 특정 상품을 찜한 기록이 있는지 조회한다.
     *
     * status가 0이든 1이든 기존 데이터가 있는지 확인할 때 사용한다.
     */
    Optional<ProductWish> findByMemberSeqAndProductSeq(Long memberSeq, Long productSeq);

    /**
     * 특정 회원이 특정 상품을 현재 찜 중인지 확인한다.
     *
     * status = 0인 데이터가 있으면 true
     */
    boolean existsByMemberSeqAndProductSeqAndStatus(
            Long memberSeq,
            Long productSeq,
            String status
    );
}