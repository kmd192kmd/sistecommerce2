package com.example.java.groupbuy.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    // 타 도메인 연관관계: 우선 Long ID로 매핑하여 컴파일/JPA 기동 에러 방지
    @Column(name = "product_seq", nullable = false)
    private Long productSeq;

    /* [연관관계 매핑 전환용 주석] Product 엔티티 완성 시 주석 해제 후 productSeq 필드 제거
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_seq", insertable = false, updatable = false)
    private Product product;
    */

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

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
