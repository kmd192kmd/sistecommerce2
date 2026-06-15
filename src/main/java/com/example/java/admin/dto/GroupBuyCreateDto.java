package com.example.java.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.java.groupbuy.entity.GroupBuyStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBuyCreateDto {

	// 등록 폼에서 보여주기위해 사용
    private String productName;
    private String sellerName;
    private String sellerPhone;
    private Integer supplyPrice;

    private List<GroupBuyOptionDto> options;
    
    // 공동구매 생성시 필요
    private Long seq;
    private Long productSeq;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer minCount;
    private Integer maxCount;
    private Integer originalPrice;
    private Integer finalPrice;
    private GroupBuyStatus status;
}