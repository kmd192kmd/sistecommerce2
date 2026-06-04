package com.example.java.groupbuy.entity;

import java.time.LocalDateTime;
import com.example.java.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group_buy")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupBuy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_buy_seq")
    @SequenceGenerator(name = "group_buy_seq", sequenceName = "group_buy_seq", allocationSize = 1)
    private Long seq;

    // product ~ groupbuy
    // 공동 구매 대상 상품(FK)
    // 등록 시 FK만 세팅하므로 LAZY 로딩
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_seq", nullable = false)
    private Product product;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "min_count", nullable = false)
    private Integer minCount;

    @Column(name = "max_count", nullable = false)
    private Integer maxCount;

    @Column(name = "original_price", nullable = false)
    private Integer originalPrice;

    @Column(name = "final_price", nullable = false)
    private Integer finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GroupBuyStatus status;
}
