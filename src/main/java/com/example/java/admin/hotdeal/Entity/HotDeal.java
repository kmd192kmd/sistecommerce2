package com.example.java.admin.hotdeal.Entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "hot_deal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HotDeal {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hot_deal_seq_gen")
    @SequenceGenerator(
            name = "hot_deal_seq_gen",
            sequenceName = "hot_deal_seq",
            allocationSize = 1
    )
    @Column(name = "seq")
    private Long seq;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    // 상태: 대기(0) / 진행중(1) / 종료(2)
    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "discount_rate")
    private Integer discountRate;

    @Column(name = "discount_price")
    private Integer discountPrice;

    // 비즈니스 로직: 핫딜 상태 변경
    public void updateStatus(Integer status) {
        this.status = status;
    }
    
    public void update(String name, LocalDateTime startDate, LocalDateTime endDate, Integer discountRate, Integer
    		  discountPrice) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.discountRate = discountRate;
        this.discountPrice = discountPrice;
    }
}