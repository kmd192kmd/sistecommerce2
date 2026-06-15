package com.example.java.product.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.java.product.entity.Product;

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
public class ProductDto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    // =========================
    // 상품 기본 정보 영역
    // =========================

    private Long seq;
    private Long sellerSeq;
    private Long categorySeq;

    private String productName;
    private Integer price;
    private String content;

    /*
        판매 상태

        예:
        ON_SALE  : 판매중
        SOLD_OUT : 품절
        STOPPED  : 판매중지
    */
    private String saleStatus;

    /*
        승인 상태

        예:
        PENDING  : 대기
        APPROVED : 승인
        REJECTED : 반려
    */
    private String approvalStatus;

    /*
        숨김 여부

        Y: 숨김
        N: 노출
    */
    private String hideYn;

    private Long viewCount;
    private Double avgRating;
    private Long reviewCount;
    private Long salesCount;

    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    /*
        상품 데이터 상태

        예:
        NORMAL
        DELETED

        주의:
        아래에 getStatus() 메서드를 따로 만들었기 때문에
        화면에서 product.status로 접근하면 saleStatus가 반환됩니다.

        실제 DB 상태값이 필요하면 getProductStatus()를 사용합니다.
    */
    private String status;


    // =========================
    // 화면 출력용 추가 정보 영역
    // =========================

    /*
        상품 대표 이미지

        Thymeleaf 화면에서 product.image로 사용할 수 있습니다.
    */
    private String image;

    /*
        상품 옵션 문자열 목록

        예:
        ["블랙 / M", "화이트 / L (+1000원)"]
    */
    @Builder.Default
    private List<String> options = new ArrayList<>();

    /*
        상품 대표 썸네일 이미지 URL

        ProductDetailDto에 있던 thumbnailUrl을
        ProductDto로 합친 필드입니다.
    */
    private String thumbnailUrl;

    /*
        로그인한 회원이 해당 상품을 찜했는지 여부

        true  : 찜함
        false : 찜 안 함
    */
    private boolean wished;

    /*
        인기 상품 순위 변동 정보
        예: "▲1", "▼2", "-", "NEW"
    */
    private String rankChange;

    /*
        순위가 이전 순위보다 가장 많이 상승한 상품 여부 (HOT 배지 표시용)
    */
    private boolean hot;

    /*
        핫딜 진행중 여부
    */
    private boolean hotDeal;
    private Integer originalPrice;
    private Integer discountRate;


    // =========================
    // 연관 데이터 목록 영역
    // =========================

    /*
        상품 이미지 목록

        상세 화면에서 여러 이미지를 출력할 때 사용합니다.
    */
    @Builder.Default
    private List<ProductImageDto> imageList = new ArrayList<>();

    /*
        상품 옵션 목록

        상세 화면에서 옵션 상세 정보를 출력할 때 사용합니다.
    */
    @Builder.Default
    private List<ProductOptionDto> optionList = new ArrayList<>();


    // =========================
    // Thymeleaf 화면 출력용 메서드 영역
    // =========================

    /*
        화면에서 product.name 형태로 접근하고 싶을 때 사용합니다.

        실제 상품명 필드는 productName입니다.
    */
    public String getName() {
        return this.productName;
    }

    /*
        평균 별점을 별 모양 문자열로 변환합니다.

        예:
        avgRating = 4.0
        결과: ★★★★☆
    */
    public String getRating() {

        if (avgRating == null) {
            return "☆☆☆☆☆";
        }

        int stars = (int) Math.round(avgRating);

        return "★".repeat(Math.max(0, Math.min(5, stars)))
                + "☆".repeat(Math.max(0, 5 - stars));
    }

    /*
        평균 별점 숫자 반환

        화면에서 averageRating 이름으로 접근할 때 사용합니다.
    */
    public Double getAverageRating() {
        return this.avgRating;
    }

    /*
        원가 반환
    */
    public Integer getOriginalPrice() {
        return this.originalPrice != null ? this.originalPrice : this.price;
    }

    /*
        할인율 반환
    */
    public Integer getDiscountRate() {
        return this.discountRate != null ? this.discountRate : 0;
    }

    /*
        상품 설명 반환

        화면에서 description 이름으로 접근할 때 사용합니다.
    */
    public String getDescription() {
        return this.content;
    }

    /*
        판매 상태 반환

        주의:
        status 필드는 NORMAL / DELETED 같은 데이터 상태이고,
        saleStatus 필드는 ON_SALE / SOLD_OUT / STOPPED 같은 판매 상태입니다.

        화면에서 ${product.status}를 판매 상태로 사용하고 있다면
        이 메서드가 필요합니다.
    */
    public String getStatus() {
        return this.saleStatus;
    }

    /*
        실제 DB 상태값 반환

        NORMAL / DELETED 값을 가져오고 싶을 때 사용합니다.
    */
    public String getProductStatus() {
        return this.status;
    }


    // =========================
    // Entity 변환 메서드 영역
    // =========================

    /*
        DTO → Entity 변환

        상품 등록 또는 수정 시 Product 엔티티로 변환할 때 사용합니다.
    */
    public Product toEntity() {

        return Product.builder()
                .seq(this.seq)
                .sellerSeq(this.sellerSeq)
                .categorySeq(this.categorySeq)
                .productName(this.productName)
                .price(this.price)
                .content(this.content)
                .saleStatus(this.saleStatus)
                .approvalStatus(this.approvalStatus)
                .hideYn(this.hideYn)
                .viewCount(this.viewCount)
                .avgRating(this.avgRating)
                .reviewCount(this.reviewCount)
                .salesCount(this.salesCount)
                .createdDate(this.createdDate)
                .updatedDate(this.updatedDate)
                .status(this.status)
                .build();
    }


    // =========================
    // 상품 이미지 DTO
    // =========================

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageDto implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        /*
            상품 이미지 번호
        */
        private Long seq;

        /*
            연결된 상품 번호
        */
        private Long productSeq;

        /*
            이미지 URL
        */
        private String imageUrl;

        /*
            Cloudinary 같은 이미지 저장소의 public id
        */
        private String publicId;

        /*
            대표 이미지 여부

            Y: 대표 이미지
            N: 일반 이미지
        */
        private String thumbnailYn;

        /*
            이미지 출력 순서
        */
        private Integer imageOrder;

        /*
            이미지 상태

            예:
            NORMAL
            DELETED
        */
        private String status;
    }


    // =========================
    // 상품 옵션 DTO
    // =========================

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductOptionDto implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        // =========================
        // 옵션 기본 정보 영역
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

        /*
            여러 옵션 값을 조합한 화면 출력용 옵션명

            예:
            블랙 / M (+1000원)
        */
        private String optionName;
    }
}