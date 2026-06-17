package com.example.java.adminpayment.dto;

import com.example.java.adminpayment.enums.PaymentType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminPaymentDTO {
    private Long seq;
    private PaymentType type;
    private Integer status;
    private Long purchaseOrderSeq;
    
    // 추가된 필드들
    private String sellerName;
    private String sellerAccount;
    private String productName;
    private Integer quantity;
    private Long supplyPrice;
    private Long totalPrice;
}