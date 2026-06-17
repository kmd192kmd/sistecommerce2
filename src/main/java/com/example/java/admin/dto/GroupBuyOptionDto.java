package com.example.java.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupBuyOptionDto {
    private Long optionSeq;
    private String optionName;
    private Integer additionalPrice;
    
    // 공동구매 옵션 생성시 필요
    private Long groupBuySeq;
    private Integer orderQty;
}