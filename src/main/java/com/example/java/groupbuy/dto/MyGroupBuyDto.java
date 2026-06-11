package com.example.java.groupbuy.dto;

import java.time.LocalDateTime;

import com.example.java.groupbuy.entity.ParticipationStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * 마이페이지 공동구매 내역용 DTO.
 *
 * GroupBuyDto(공구 목록·상세용)와 분리하여
 * 내가 참여한 내역 화면에 필요한 필드만 담습니다.
 */
@Getter
@Builder
public class MyGroupBuyDto {

    /** 참여 seq */
    private Long participationSeq;

    /** 공구 seq */
    private Long groupBuySeq;

    /** 상품명 */
    private String productName;

    /** 대표 이미지 URL */
    private String thumbnailUrl;

    /** 옵션명 */
    private String optionName;

    /** 공구 확정가 */
    private Integer finalPrice;

    /** 공구 시작일시 */
    private LocalDateTime startAt;

    /** 공구 종료일시 */
    private LocalDateTime endAt;

    /** 참여 상태 */
    private ParticipationStatus status;

    /** 참여 신청일시 */
    private LocalDateTime createdAt;
}
