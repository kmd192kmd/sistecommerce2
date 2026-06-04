package com.example.java.groupbuy.dto;

import com.example.java.groupbuy.entity.GroupBuyStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 공구 목록 REST 응답 (GET /api/group-buys) 한 건.
 *
 * 추후 추가 예정:
 *  - currentCount / progress  : participation 집계 시
 */
@Getter
@Builder
public class GroupBuySummaryResponse {

    private Long seq;
    private GroupBuyStatus status;

    private String productName;     // Product 연동
    private String image;           // 대표 썸네일 이미지 URL (없으면 기본 이미지)

    private Integer originalPrice;
    private Integer finalPrice;
    private Integer discountRate;   // 계산값
    private Long remainSeconds;     // 계산값: 지금~마감 남은 초 (마감 지났으면 0)
    private String remainText;      // 계산값: remainSeconds를 "N일 N시간" 식으로 표기 (화면용)
    private Integer minCount;

    // participation 집계 구현 전까지 임시 0 (현재 참여 인원 / 진행률 %)
    private Integer currentCount;
    private Integer progress;
}
