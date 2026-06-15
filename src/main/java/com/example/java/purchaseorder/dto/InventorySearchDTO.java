package com.example.java.purchaseorder.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InventorySearchDTO {

    private String saleStatus;
    // 전체 / UNDER_SAFE
    private String stockType;
    private Integer priceFrom;
    private Integer priceTo;
    private LocalDateTime createdDateFrom;
    private LocalDateTime createdDateTo;
    private String keyword;
}
