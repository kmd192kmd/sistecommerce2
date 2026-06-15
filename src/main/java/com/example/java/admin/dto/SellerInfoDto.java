package com.example.java.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SellerInfoDto {
    private String sellerName;
    private String sellerPhone;
    private Integer supplyRate;
}