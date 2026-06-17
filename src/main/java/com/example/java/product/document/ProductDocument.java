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

    @Field(type = FieldType.Long)
    private Long sellerSeq;
    
    @Field(type = FieldType.Long)
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

    @Field(type = FieldType.Integer)
    private Integer price;

    @Field(type = FieldType.Keyword)
    private String saleStatus;

    @Field(type = FieldType.Keyword)
    private String approvalStatus;

    @Field(type = FieldType.Keyword)
    private String hideYn;

    @Field(type = FieldType.Long)
    private Long viewCount;

    @Field(type = FieldType.Double)
    private Double avgRating;

    @Field(type = FieldType.Long)
    private Long reviewCount;

    @Field(type = FieldType.Long)
    private Long salesCount;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdDate;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text)
    private String thumbnailUrl;

    @Field(type = FieldType.Dense_Vector, dims = 128)
    private float[] embedding;
}

