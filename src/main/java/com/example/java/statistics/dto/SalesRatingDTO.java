package com.example.java.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SalesRatingDTO {
    private String name;     // 상품명, 카테고리명, 판매처명
    private Long totalSales; // 판매량
}