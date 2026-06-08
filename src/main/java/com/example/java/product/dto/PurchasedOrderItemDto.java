package com.example.java.product.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchasedOrderItemDto {

    private Long orderItemSeq;
    private String productName;
    private String optionName;
}