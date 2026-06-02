package com.example.java.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.java.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 기본 목록 조회: 숨김이 아니고 판매중지(STOPPED) 상태가 아니며 정상 상태인 상품 검색 + 페이징/정렬 지원
    Page<Product> findByHideYnAndSaleStatusNotAndStatus(String hideYn, String saleStatus, String status, Pageable pageable);

    // 카테고리별 필터링
    Page<Product> findByCategorySeqAndHideYnAndSaleStatusNotAndStatus(Long categorySeq, String hideYn, String saleStatus, String status, Pageable pageable);

    // 상품명 기준 검색 (대소문자 및 공백 무시)
    @Query("SELECT p FROM Product p WHERE LOWER(REPLACE(p.productName, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:keyword, ' ', ''), '%')) " +
           "AND p.hideYn = 'N' AND p.saleStatus <> 'STOPPED' AND p.status = 'NORMAL'")
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    // 카테고리 및 상품명 기준 검색 (대소문자 및 공백 무시)
    @Query("SELECT p FROM Product p WHERE p.categorySeq = :categorySeq " +
           "AND LOWER(REPLACE(p.productName, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:keyword, ' ', ''), '%')) " +
           "AND p.hideYn = 'N' AND p.saleStatus <> 'STOPPED' AND p.status = 'NORMAL'")
    Page<Product> searchByCategoryAndName(@Param("categorySeq") Long categorySeq, @Param("keyword") String keyword, Pageable pageable);
}
