package com.example.java.admin.hotdeal.Entity;

import com.example.java.product.entity.Options;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hot_deal_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HotDealProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hot_deal_product_seq_gen")
    @SequenceGenerator(
            name = "hot_deal_product_seq_gen",
            sequenceName = "hot_deal_product_seq",
            allocationSize = 1
    )
    @Column(name = "seq")
    private Long seq;

    // 핫딜(부모)과의 단방향 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hot_deal_seq", nullable = false)
    private HotDeal hotDeal;

    // 옵션(부모)과의 단방향 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "options_seq", nullable = false)
    private Options options; // 주의: Options 엔티티가 미리 뼈대라도 만들어져 있어야 합니다!

    @Column(name = "hot_deal_stock")
    private Integer hotDealStock;

    @Column(name = "sold_quantity")
    private Integer soldQuantity;
    
    // 비즈니스 로직: 판매 수량 증가
    public void addSoldQuantity(int quantity) {
        this.soldQuantity = (this.soldQuantity == null ? 0 : this.soldQuantity) + quantity;
    }
}