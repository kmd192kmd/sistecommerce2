package com.example.java.product.document;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.InnerField;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(indexName = "product")
@Setting(settingPath = "elasticsearch/settings.json")
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

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer"),
        otherFields = {
            @InnerField(suffix = "autocomplete", type = FieldType.Text, analyzer = "autocomplete_analyzer"),
            @InnerField(suffix = "chosung", type = FieldType.Text, analyzer = "chosung_analyzer")
        }
    )
    private String productName;

    @Field(type = FieldType.Text, analyzer = "chosung_analyzer")
    private String productNameChosung;

    private Integer price;

    @Field(type = FieldType.Keyword)
    private String saleStatus;

    @Field(type = FieldType.Keyword)
    private String approvalStatus;

    @Field(type = FieldType.Keyword)
    private String hideYn;

    private Long viewCount;

    private Double avgRating;

    private Long reviewCount;

    private Long salesCount;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdDate;

    @Field(type = FieldType.Keyword)
    private String status;

    private String thumbnailUrl;

    @Field(type = FieldType.Dense_Vector, dims = 128)
    private float[] embedding;
}

