package com.example.java.cart.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {

    private Long seq;

    private Long memberSeq;
    private Long optionsSeq;

    private int quantity;

    private LocalDate createdDate;

    // ── 상품 정보 (조회 시 함께 표시) ──
    private Long productSeq;
    private String productName;
    private int price;
    private String thumbnailUrl;

    // ── 옵션 정보 ──
    private String optionName;   // Options.getDisplayName() 결과
    private int additionalPrice;
    private int stock;

    // ── 계산 필드 ──
    public int getItemTotal() {
        return (price + additionalPrice) * quantity;
    }

}
