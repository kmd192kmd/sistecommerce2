package com.example.java.groupbuy.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.java.groupbuy.entity.GroupBuyStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 공구 상세 REST 응답 (GET /api/group-buys/{id}).
 * React 구매 패널이 소비한다.
 *
 * 추후 추가 예정:
 *  - currentCount / progress : participation 집계 시 (현재 참여 인원/진행률)
 */
@Getter
@Builder
public class GroupBuyDetailResponse {

    private Long seq;
    private GroupBuyStatus status;

    private String productName;     // Product 연동
    private String image;           // 대표 썸네일 이미지 URL (없으면 기본 이미지)
    private String description;     // 상품 설명 (product.content)

    private Integer originalPrice;
    private Integer finalPrice;
    private Integer discountRate;   // 계산값: (정가-할인가) / 정가 * 100 (정수 %)

    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long remainSeconds;     // 계산값: 지금~마감 남은 초 (마감 지났으면 0)

    private Integer minCount;
    private Integer maxCount;

    private List<GroupBuyOptionView> options;
}
