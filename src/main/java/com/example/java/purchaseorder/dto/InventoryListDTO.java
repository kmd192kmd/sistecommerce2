package com.example.java.purchaseorder.dto;


import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryListDTO {
	private Long optionsSeq;
	private String saleStatus;
	private String productName;
	private Integer price;
	private String optionsName;
	private Integer stock;
	private Integer safetyStock;
	private String sellerName;
	private LocalDateTime createdDate;
}
