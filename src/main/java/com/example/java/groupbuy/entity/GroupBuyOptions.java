package com.example.java.groupbuy.entity;

import com.example.java.product.entity.Options;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "group_buy_options")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupBuyOptions {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_buy_options_seq")
    @SequenceGenerator(name = "group_buy_options_seq", sequenceName = "group_buy_options_seq", allocationSize = 1)
    private Long seq;

    // 우리 도메인 내의 엔티티이므로 안전하게 연관관계 매핑 적용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_seq", nullable = false)
    private GroupBuy groupBuy;
    
    // options ~ group_buy_options
    // 이 공구 옵션이 매핑된 상품 옵션(FK) 
    // 등록 시 FK만 세팅하므로 LAZY 로딩
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "options_seq", nullable = false)
    private Options options;

    @Column(name = "order_qty", nullable = false)
    private Integer orderQty;

    @Column(name = "occupied_count", nullable = false)
    private Integer occupiedCount;
}
