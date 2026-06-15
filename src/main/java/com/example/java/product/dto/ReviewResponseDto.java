package com.example.java.product.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDto {

    private Long seq;
    private Long productSeq;
    private Long memberSeq;
    private Long orderItemSeq;

    private Integer rating;
    private String content;

    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    private String status;

    private List<ReviewImageDto> images;
    
    private String nickname;
}