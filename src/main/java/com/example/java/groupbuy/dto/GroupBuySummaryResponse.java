package com.example.java.groupbuy.dto;

import com.example.java.groupbuy.entity.GroupBuyStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 공구 목록 REST 응답 (GET /api/group-buys) 한 건.
 *
 * 추후 추가 예정:
 *  - productName / image      : Product 연동 시
 *  - currentCount / progress  : participation 집계 시
 */
@Getter
@Builder
public class GroupBuySummaryResponse {

    private Long seq;
    private GroupBuyStatus status;

    private Integer originalPrice;
    private Integer finalPrice;
    private Integer discountRate;   // 계산값
    private Long remainSeconds;     // 계산값: 지금~마감 남은 초 (마감 지났으면 0)
    private Integer minCount;
}
