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
    
    // group_buy ~ group_buy_options 
    // 공동구매 도메인 내의 엔티티이므로 안전하게 연관관계 매핑 적용
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

    /** 이 옵션의 매진 여부 (점유 수가 발주 가능 수량에 도달). */
    public boolean isSoldOut() {
        return occupiedCount >= orderQty;
    }

    /**
     * 점유(+1). 정규 참여 결제 시 / 대기열 승격 시 호출.
     * 반드시 비관적 락(findBySeqForUpdate)으로 행을 잠근 뒤 호출해야 동시성이 보장된다 (NFR-001).
     */
    public void occupy() {
        if (isSoldOut()) {
            throw new IllegalStateException("이미 매진된 옵션입니다. seq=" + seq);
        }
        occupiedCount++;
    }

    /**
     * 복구(-1). 참여 취소 / 결제기한 만료 / 승격자 미결제 시 호출.
     * occupiedCount가 음수로 내려가지 않도록 방어한다 (NFR-003 정합성).
     */
    public void release() {
        if (occupiedCount <= 0) {
            throw new IllegalStateException("점유 수가 0이라 복구할 수 없습니다. seq=" + seq);
        }
        occupiedCount--;
    }
}
