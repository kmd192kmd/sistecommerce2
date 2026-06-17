package com.example.java.stockhistory.dto;

import java.time.LocalDateTime;

import com.example.java.product.entity.Options;
import com.example.java.stockhistory.entity.StockHistory;
import com.example.java.stockhistory.enums.StockHistorySourceType;
import com.example.java.stockhistory.enums.StockHistoryType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockHistoryListDTO {

    // StockHistory 기본 정보
	private Long seq;
    private StockHistoryType type;
    private String reason;
    private StockHistorySourceType sourceType;
    private LocalDateTime createdAt;

    // 수량 정보
    private int quantity;
    private int beforeStock;
    private int afterStock;

    // 옵션 / 상품 정보 (fetch join 대상)
    private String saleStatus;
    private String productName;
    private String optionsName;
    
    public static StockHistoryListDTO from(StockHistory entity) {

        Options options = entity.getOptions();

        return StockHistoryListDTO.builder()
        		.seq(entity.getSeq())
				.type(entity.getType())
                .reason(entity.getReason())
                .sourceType(entity.getSourceType())
                .createdAt(entity.getCreatedAt())
                .quantity(entity.getQuantity())
                .beforeStock(entity.getBeforeStock())
                .afterStock(entity.getAfterStock())
                // 연관관계 타고 접근 (fetch join 전제)
                .saleStatus(options.getProduct().getSaleStatus())
                .productName(options.getProduct().getProductName())
                .optionsName(options.getDisplayName())
                .build();
    }
}