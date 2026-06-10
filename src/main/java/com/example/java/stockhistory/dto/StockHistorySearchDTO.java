package com.example.java.stockhistory.dto;

import java.time.LocalDateTime;

import com.example.java.stockhistory.enums.StockHistoryType;

import lombok.Data;

@Data
public class StockHistorySearchDTO {

    private String saleStatus;   			// 상품 상태
    private StockHistoryType type;         // IN / OUT

    private Long priceFrom;
    private Long priceTo;

    private LocalDateTime changedDateFrom; // 재고 변경 시작
    private LocalDateTime changedDateTo;   // 재고 변경 종료

    private String keyword; // productName 검색
}