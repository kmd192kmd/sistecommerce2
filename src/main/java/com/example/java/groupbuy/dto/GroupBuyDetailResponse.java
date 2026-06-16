package com.example.java.groupbuy.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.java.groupbuy.entity.GroupBuyStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 공구 상세 REST 응답 (GET /api/group-buys/{id}).
 * React 구매 패널이 소비한다. 
 * (React 도입 전엔 detail.html 서버렌더 fallback도 같은 필드를 쓴다)
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
    private String remainText;      // 계산값: 남은 초를 "N일 N시간" 식으로 표기 (마감 시 "마감")

    private Integer minCount;
    private Integer maxCount;
    private Integer currentCount;   // 집계값: 현재 정규 참여 인원 (PARTICIPATING 수)
    private Integer progress;       // 계산값: currentCount / minCount * 100 (최대 100%)

    private List<GroupBuyOptionView> options;
}
