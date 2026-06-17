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

    /**
     * 공구 시작: 시작 시각이 도래해 모집을 개시한다. SCHEDULED → ONGOING.
     */
    public void open() {
        this.status = GroupBuyStatus.ONGOING;
    }

    /**
     * 마감 확정: 최소 인원을 달성해 공구가 성사됨. ONGOING → CONFIRMED.
     * finishedAt에 실제 마감 처리 시각을 기록한다.
     */
    public void confirm(LocalDateTime finishedAt) {
        this.status = GroupBuyStatus.CONFIRMED;
        this.finishedAt = finishedAt;
    }

    /**
     * 마감 무산: 최소 인원 미달로 공구가 무산됨. ONGOING → FAILED.
     * 결제 완료자 전원 환불은 서비스에서 처리한다.
     */
    public void fail(LocalDateTime finishedAt) {
        this.status = GroupBuyStatus.FAILED;
        this.finishedAt = finishedAt;
    }

    /**
     * 관리자 강제 중단: ONGOING/SCHEDULED → STOPPED
     */
    public void forceStop(LocalDateTime finishedAt) {
        this.status = GroupBuyStatus.STOPPED;
        this.finishedAt = finishedAt;
    }
}
