package com.example.java.product.dto;

import java.util.List;

import com.example.java.product.entity.Category;

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

    /*
        하위 카테고리 목록

        예:
        대분류 DTO 안에 중분류 목록
        중분류 DTO 안에 소분류 목록

        기존 기능에서 사용할 수 있으므로 유지합니다.
    */
    private List<CategoryDto> children;


    /*
        Category Entity를 CategoryDto로 변환하는 메서드

        CategoryService에서 아래 코드가 동작하려면 필요합니다.

        categoryRepository.findAll()
                .stream()
                .map(CategoryDto::from)
                .toList();
    */
    public static CategoryDto from(Category category) {

        if (category == null) {
            return null;
        }

        return CategoryDto.builder()
                .seq(category.getSeq())
                .categoryName(category.getCategoryName())
                .depthLevel(category.getDepthLevel())
                .parentSeq(category.getParentSeq())
                .children(null)
                .build();
    }
}