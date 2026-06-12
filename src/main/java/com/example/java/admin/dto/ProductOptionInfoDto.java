package com.example.java.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionInfoDto {
    private Long productSeq;
    private String productName;
    private String sellerName;
    private String priceDisplay; // e.g. "10,000원" or "10,000원 ~ 12,000원"
    private boolean hasOptions;
    private boolean isGroupBuy;
}
