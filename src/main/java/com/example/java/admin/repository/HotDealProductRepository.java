package com.example.java.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.java.admin.hotdeal.Entity.HotDealProduct;

@Repository
public interface HotDealProductRepository extends JpaRepository<HotDealProduct, Long> {
    // 💡 팁: 나중에 특정 핫딜(seq)에 묶인 상품 옵션 목록을 싹 다 끌어올 때 사용할 맞춤형 쿼리 메서드입니다.
    List<HotDealProduct> findByHotDeal_Seq(Long hotDealSeq);

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT hdp.options.product.seq " +
        "FROM HotDealProduct hdp " +
        "WHERE hdp.hotDeal.status = 1 " +
        "  AND hdp.hotDeal.startDate <= :now " +
        "  AND hdp.hotDeal.endDate >= :now"
    )
    List<Long> findActiveHotDealProductSeqs(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

    @org.springframework.data.jpa.repository.Query(
        "SELECT hdp.options.product.seq, hdp.hotDeal.discountRate, hdp.hotDeal.discountPrice " +
        "FROM HotDealProduct hdp " +
        "WHERE hdp.hotDeal.status = 1 " +
        "  AND hdp.hotDeal.startDate <= :now " +
        "  AND hdp.hotDeal.endDate >= :now"
    )
    List<Object[]> findActiveHotDealDetails(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}