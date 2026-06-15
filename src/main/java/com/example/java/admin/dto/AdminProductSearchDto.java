package com.example.java.admin.dto;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductSearchDto {
    private String saleStatus;
    private String status;
    private Long categorySeq;
    private Integer priceFrom;
    private Integer priceTo;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime createdDateFrom;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime createdDateTo;
    
    private String searchType; // "productName" or "sellerName"
    private String keyword;
}
