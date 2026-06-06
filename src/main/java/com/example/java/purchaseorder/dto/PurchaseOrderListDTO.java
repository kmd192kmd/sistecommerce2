package com.example.java.purchaseorder.dto;

import com.example.java.purchaseorder.entity.PurchaseOrder;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseOrderListDTO {

    private Long optionsSeq;

    public static PurchaseOrderListDTO from(PurchaseOrder entity) {
        return PurchaseOrderListDTO.builder()
                .optionsSeq(
                    entity.getOptions() != null 
                        ? entity.getOptions().getSeq() 
                        : null
                )
                .build();
    }
}