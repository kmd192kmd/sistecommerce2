package com.example.java.mypage.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyReviewDto {

    private Long reviewSeq;
    private Long productSeq;
    private String productName;
    private String productImageUrl;
    private Integer rating;
    private String content;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
