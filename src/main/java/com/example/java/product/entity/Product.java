package com.example.java.product.entity;

import java.time.LocalDateTime;

import com.example.java.product.dto.ProductDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @Column(name = "seq")
    @SequenceGenerator(name = "product_seq", allocationSize = 1, sequenceName = "product_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    private Long seq;

    /*
        판매자 번호

        지금은 Seller 엔티티 연관관계를 쓰지 않고
        seller_seq 값을 Long으로 직접 관리합니다.

        이유:
        Seller 엔티티 이름이나 패키지가 정확히 맞지 않으면
        @ManyToOne에서 컴파일 오류가 날 수 있기 때문입니다.
    */
    @Column(name = "seller_seq", nullable = false)
    private Long sellerSeq;

    /*
        카테고리 번호

        지금은 Category 엔티티 연관관계를 쓰지 않고
        category_seq 값을 Long으로 직접 관리합니다.
    */
    @Column(name = "category_seq", nullable = false)
    private Long categorySeq;

    @Column(name = "product_name", nullable = false, length = 60)
    private String productName;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /*
        판매 상태

        ON_SALE  : 판매중
        SOLD_OUT : 품절
        STOPPED  : 판매중지
    */
    @Column(name = "sale_status", nullable = false, length = 20)
    @Builder.Default
    private String saleStatus = "ON_SALE";

    /*
        승인 상태

        PENDING  : 대기
        APPROVED : 승인
        REJECTED : 반려
    */
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private String approvalStatus = "PENDING";

    /*
        숨김 여부

        Y: 숨김
        N: 노출
    */
    @Column(name = "hide_yn", nullable = false, length = 1)
    @Builder.Default
    private String hideYn = "N";

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "avg_rating", nullable = false)
    @Builder.Default
    private Double avgRating = 0.0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Long reviewCount = 0L;

    @Column(name = "sales_count", nullable = false)
    @Builder.Default
    private Long salesCount = 0L;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    /*
        상품 데이터 상태

        NORMAL  : 정상
        DELETED : 삭제
    */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "NORMAL";

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /*
        INSERT 전에 자동 실행됩니다.

        상품 등록 시 기본값이 null이면 기본값을 채웁니다.
    */
    @PrePersist
    protected void onCreate() {

        this.createdDate = LocalDateTime.now();

        if (this.saleStatus == null) this.saleStatus = "ON_SALE";
        if (this.approvalStatus == null) this.approvalStatus = "PENDING";
        if (this.hideYn == null) this.hideYn = "N";
        if (this.viewCount == null) this.viewCount = 0L;
        if (this.avgRating == null) this.avgRating = 0.0;
        if (this.reviewCount == null) this.reviewCount = 0L;
        if (this.salesCount == null) this.salesCount = 0L;
        if (this.status == null) this.status = "NORMAL";
    }

    /*
        UPDATE 전에 자동 실행됩니다.

        상품 수정 시 updated_date를 현재 시간으로 변경합니다.
    */
    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }

    /*
        Entity → DTO 변환

        Service에서 화면에 내려줄 ProductDto로 변환할 때 사용합니다.
    */
    public ProductDto toDto() {

        String thumb = this.thumbnailUrl;
        if (thumb == null || thumb.isBlank()) {
            thumb = "/src/images/product/default.png";
        }

        return ProductDto.builder()
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
                .thumbnailUrl(thumb)
                .image(thumb)
                .build();
    }
}