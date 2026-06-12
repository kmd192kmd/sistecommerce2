package com.example.java.admin.dto;

import java.time.LocalDateTime;

import com.querydsl.core.annotations.QueryProjection;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdminProductListDto {
    
    private Long optionsSeq;
    private Long productSeq;
    private String saleStatus;
    private String productName;
    private Integer price;
    private String optionsName;
    private String thumbnailUrl;
    private String sellerName;
    private LocalDateTime createdDate;

    @QueryProjection
    public AdminProductListDto(Long optionsSeq, Long productSeq, String saleStatus, String productName, 
                               Integer price, String optionsName, String thumbnailUrl, 
                               String sellerName, LocalDateTime createdDate) {
        this.optionsSeq = optionsSeq;
        this.productSeq = productSeq;
        this.saleStatus = saleStatus;
        this.productName = productName;
        this.price = price;
        this.optionsName = optionsName;
        this.thumbnailUrl = thumbnailUrl;
        this.sellerName = sellerName;
        this.createdDate = createdDate;
    }
}
