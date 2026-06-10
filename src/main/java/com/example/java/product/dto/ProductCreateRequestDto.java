package com.example.java.product.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCreateRequestDto {

    // =========================
    // 상품 기본 정보 영역
    // =========================
    private Long sellerSeq;
    private Long categorySeq;

    private String productName;
    private Integer price;
    private String content;

    // 옵션을 선택하지 않은 상품의 기본 재고입니다.
    //
    // 현재 product 테이블에는 상품 수량 컬럼이 없고,
    // 재고는 options 테이블의 stock 컬럼에 저장됩니다.
    //
    // 따라서 옵션을 선택하지 않은 경우에도
    // 사용자가 입력한 상품 수량을 options 테이블에 기본 옵션 1개로 저장하기 위해 사용합니다.
    //
    // HTML의 name="stock"과 연결됩니다.
    // =========================
    private Integer stock;

    // =========================
    // 관리자 배정 영역
    // product_request.admin_seq에 들어감
    // 테스트할 때는 1 넣으면 됨
    // =========================
    private Long adminSeq;

    // =========================
    // 상품 이미지 영역
    // =========================
    private List<ProductImageRequestDto> imageList = new ArrayList<>();

    // =========================
    // 상품 옵션 영역
    // =========================
    private List<ProductOptionRequestDto> optionList = new ArrayList<>();

    // =========================
    // 추가된 부분
    // =========================
    // 화면에서 동적으로 생성되는 옵션 입력값을 받는 영역입니다.
    //
    // 현재 상품 등록 HTML에서는 옵션을 optionList[0].color 같은 형태로 보내지 않고,
    // 아래와 같은 name으로 각각 따로 보냅니다.
    //
    // optionTypes       : 옵션 종류, 예) 색상, 사이즈, 맛
    // optionNames       : 옵션 값, 예) 블랙, M, 매운맛
    // optionPrices      : 추가금액
    // optionStocks      : 재고
    // optionSafeStocks  : 안전재고
    //
    // 그래서 Service에서 이 값들을 ProductOptionRequestDto 목록으로 변환합니다.
    // =========================
    private List<String> optionTypes = new ArrayList<>();
    private List<String> optionNames = new ArrayList<>();
    private List<Integer> optionPrices = new ArrayList<>();
    private List<Integer> optionStocks = new ArrayList<>();
    private List<Integer> optionSafeStocks = new ArrayList<>();


    @Getter
    @Setter
    public static class ProductImageRequestDto {

        private String imageUrl;
        private String publicId;

        // Y / N
        private String thumbnailYn;

        private Integer imageOrder;
    }


    @Getter
    @Setter
    public static class ProductOptionRequestDto {

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
    }
}