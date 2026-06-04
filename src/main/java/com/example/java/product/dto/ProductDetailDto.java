package com.example.java.product.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDetailDto {

    // =========================
    // 상품 기본 정보 영역
    // =========================
    private Long seq;
    private Long sellerSeq;
    private Long categorySeq;

    private String productName;
    private Integer price;
    private String content;

    private String saleStatus;
    private String approvalStatus;
    private String hideYn;
    private String status;

    private Integer viewCount;
    private Double avgRating;
    private Integer reviewCount;
    private Integer salesCount;

    private String createdDate;
    private String updatedDate;

    // =========================
    // 화면 출력용 추가 정보 영역
    // =========================
    private String thumbnailUrl;
    private boolean wished;

    // =========================
    // 연관 데이터 목록 영역
    // =========================
    private List<ProductImageDto> imageList = new ArrayList<>();
    private List<ProductOptionDto> optionList = new ArrayList<>();


    @Getter
    @Setter
    public static class ProductImageDto {

        // =========================
        // 상품 이미지 정보 영역
        // =========================
        private Long seq;
        private Long productSeq;

        private String imageUrl;
        private String publicId;
        private String thumbnailYn;

        private Integer imageOrder;
        private String status;
    }


    @Getter
    @Setter
    public static class ProductOptionDto {

        // =========================
        // 상품 옵션 기본 정보 영역
        // =========================
        private Long seq;
        private Long productSeq;

        // =========================
        // 옵션 항목 영역
        // =========================
        private String color;
        private String optionsSize;
        private String volumeWeight;
        private String taste;
        private String storageType;
        private String scentIngredient;
        private String voltage;
        private String quantitySet;
        private String sizeSpec;
        private String storageCapacity;
        private String memory;
        private String switchAxis;
        private String connectionType;
        private String wearableSpec;
        private String materialType;
        private String optionsType;

        // =========================
        // 재고 / 가격 영역
        // =========================
        private Integer stock;
        private Integer safetyStock;
        private Integer additionalPrice;

        // =========================
        // 화면 출력용 영역
        // =========================
        private String optionName;
    }
}