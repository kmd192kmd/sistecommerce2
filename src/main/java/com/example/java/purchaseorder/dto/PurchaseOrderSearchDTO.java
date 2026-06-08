package com.example.java.purchaseorder.dto;

import java.time.LocalDate;

import com.example.java.purchaseorder.enums.PurchaseOrderStatus;

import lombok.Data;

@Data
public class PurchaseOrderSearchDTO {

    private PurchaseOrderStatus status;
    private Long totalPriceFrom;
    private Long totalPriceTo;
    private LocalDate orderDateFrom;
    private LocalDate orderDateTo;
    private LocalDate expectedDateFrom;
    private LocalDate expectedDateTo;
    // TODO 옵션명 검색방법 생각!
    private String keywordType; // 상품명 (판매처, 옵션명 추가 예정)
    private String keyword;
}