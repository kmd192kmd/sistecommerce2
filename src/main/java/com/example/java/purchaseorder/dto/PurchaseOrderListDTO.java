package com.example.java.purchaseorder.dto;

import com.example.java.product.entity.Options;
import com.example.java.purchaseorder.entity.PurchaseOrder;
import com.example.java.purchaseorder.enums.PurchaseOrderStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseOrderListDTO {

	private Long seq;
    private PurchaseOrderStatus status;
    private int quantity;

    private Long optionsSeq;
    private Integer stock;
    private Integer safetyStock;

    public static PurchaseOrderListDTO from(PurchaseOrder entity) {

        Options options = entity.getOptions();

        return PurchaseOrderListDTO.builder()
                .seq(entity.getSeq())
                .status(entity.getStatus())
                .quantity(entity.getQuantity())

                .optionsSeq(options != null ? options.getSeq() : null)
                .stock(options != null ? options.getStock() : null)
                .safetyStock(options != null ? options.getSafetyStock() : null)
                .build();
    }
}