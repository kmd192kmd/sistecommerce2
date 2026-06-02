package com.example.java.product.dto;

import java.time.LocalDateTime;
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
public class ProductDto {
    private Long seq;
    private Long sellerSeq;
    private Long categorySeq;
    private String productName;
    private Integer price;
    private String content;
    private String saleStatus;
    private String approvalStatus;
    private String hideYn;
    private Long viewCount;
    private Double avgRating;
    private Long reviewCount;
    private Long salesCount;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String status;

    // 추가 필드 (Thymeleaf 템플릿 뷰 렌더링에 사용)
    private String image;
    private List<String> options;

    public String getName() {
        return this.productName;
    }

    public String getRating() {
        if (avgRating == null) return "☆☆☆☆☆";
        int stars = (int) Math.round(avgRating);
        return "★".repeat(Math.max(0, Math.min(5, stars))) + "☆".repeat(Math.max(0, 5 - stars));
    }

    public Double getAverageRating() {
        return this.avgRating;
    }

    public Integer getOriginalPrice() {
        return this.price;
    }

    public Integer getDiscountRate() {
        return 0;
    }

    public String getDescription() {
        return this.content;
    }

    public String getStatus() {
        return this.saleStatus;
    }

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
}
