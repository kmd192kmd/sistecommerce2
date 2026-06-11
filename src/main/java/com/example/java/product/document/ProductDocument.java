package com.example.java.product.document;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(indexName = "product")
@Setting(shards = 1, replicas = 0)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {

    @Id
    private Long id; // product seq

    private Long sellerSeq;
    
    private Long categorySeq;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String productName;

    private Integer price;

    private String saleStatus;

    private String approvalStatus;

    private String hideYn;

    private Long viewCount;

    private Double avgRating;

    private Long reviewCount;

    private Long salesCount;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdDate;

    private String status;

    private String thumbnailUrl;
}
