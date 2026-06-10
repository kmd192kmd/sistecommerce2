package com.example.java.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStatisticsDto {
    private Long totalRevenue; // 총 결제금액 (고객 매출)
    private Long totalPayout;  // 누적 정산액 (판매자 총 지급 대금)
    private Long netProfit;    // 쇼핑몰 순수익
}
