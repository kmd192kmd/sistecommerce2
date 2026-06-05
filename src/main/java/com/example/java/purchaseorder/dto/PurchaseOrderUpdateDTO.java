package com.example.java.purchaseorder.dto;

import java.util.List;

import com.example.java.purchaseorder.enums.PurchaseOrderStatus;

import lombok.Data;

@Data
public class PurchaseOrderUpdateDTO {
	private List<Long> seqs;
    private PurchaseOrderStatus status;
}
