package com.example.java.product.dto;

import java.util.List;

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
public class CategoryDto {
    private Long seq;
    private String categoryName;
    private Integer depthLevel;
    private Long parentSeq;
    private List<CategoryDto> children;
}
