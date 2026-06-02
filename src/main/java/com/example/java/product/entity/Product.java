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

    @Column(name = "seller_seq", nullable = false)
    private Long sellerSeq;

    @Column(name = "category_seq", nullable = false)
    private Long categorySeq;

    @Column(name = "product_name", nullable = false, length = 60)
    private String productName;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "sale_status", nullable = false, length = 20)
    @Builder.Default
    private String saleStatus = "ON_SALE"; // ON_SALE, SOLD_OUT, STOPPED

    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private String approvalStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "hide_yn", nullable = false, length = 1)
    @Builder.Default
    private String hideYn = "N"; // Y, N

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

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "NORMAL"; // NORMAL, DELETED

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

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }

    public ProductDto toDto() {
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
                .build();
    }
}
