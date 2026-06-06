package com.example.java.purchaseorder.dto;

import java.time.LocalDate;

import com.example.java.purchaseorder.enums.PurchaseOrderType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderCreateDTO {

    private int quantity;
    private Long supplyPrice;
    private Long totalPrice;

    private LocalDate orderDate;
    private LocalDate expectedDate;

    private PurchaseOrderType type;

    private Long optionsSeq;
    private Long groupBuyOptionsSeq;
}