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
    
    // 판매처 정보 추가 (엔티티 변경 없이 Service에서 주입)
    private String sellerName;
    private String productName;
    private String optionsName;
    
    private int quantity;
    private Long totalPrice;

    private LocalDate orderDate;
    private LocalDate expectedDate;
    private LocalDate receivedDate;

    public static PurchaseOrderListDTO from(PurchaseOrder entity, String sellerName) {

        Options options = entity.getOptions();

        return PurchaseOrderListDTO.builder()
                .seq(entity.getSeq())
                .status(entity.getStatus())
                .sellerName(sellerName)
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