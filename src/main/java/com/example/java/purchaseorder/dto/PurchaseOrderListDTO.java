package com.example.java.purchaseorder.dto;

import java.time.LocalDate;

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
    
    // TODO seller 엔티티 생기면 추가
//    private String sellerName;
    private String productName;
    private String optionsName;
    
    private int quantity;
    private Long totalPrice;

    private LocalDate orderDate;
    private LocalDate expectedDate;
    private LocalDate receivedDate;

    public static PurchaseOrderListDTO from(PurchaseOrder entity) {

        Options options = entity.getOptions();

        return PurchaseOrderListDTO.builder()
                .seq(entity.getSeq())
                .status(entity.getStatus())
                .productName(options.getProduct().getProductName())
                .optionsName(options.getDisplayName())
                .quantity(entity.getQuantity())
                .totalPrice(entity.getTotalPrice())
                .orderDate(entity.getOrderDate())
                .expectedDate(entity.getExpectedDate())
                .receivedDate(entity.getReceivedDate())
                .build();
    }
}