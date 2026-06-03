package com.example.java.product.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.java.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 필터 조건(카테고리, 검색 키워드, 최소/최대 가격, 최소 평점)을 모두 아우르는 통합 검색 쿼리
    @Query("SELECT p FROM Product p WHERE " +
           "(:categorySeqs IS NULL OR p.categorySeq IN :categorySeqs) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(REPLACE(p.productName, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:keyword, ' ', ''), '%'))) " +
           "AND (p.price >= :minPrice) " +
           "AND (p.price <= :maxPrice) " +
           "AND (p.avgRating >= :minRating) " +
           "AND (:saleStatus IS NULL OR p.saleStatus = :saleStatus) " +
           "AND p.hideYn = 'N' AND p.saleStatus <> 'STOPPED' AND p.status = 'NORMAL'")
    Page<Product> findWithFilters(
            @Param("categorySeqs") List<Long> categorySeqs,
            @Param("keyword") String keyword,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("minRating") Double minRating,
            @Param("saleStatus") String saleStatus,
            Pageable pageable);
}
