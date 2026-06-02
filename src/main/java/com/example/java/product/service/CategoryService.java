package com.example.java.product.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.dto.CategoryDto;
import com.example.java.product.entity.Category;
import com.example.java.product.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 0번(대분류) 카테고리 기점으로 전체 트리를 구축하여 반환 (N+1 문제 및 LazyInitializationException 완전 방지)
    public List<CategoryDto> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();

        List<Category> rootCategories = allCategories.stream()
                .filter(c -> c.getDepthLevel() == 0)
                .collect(Collectors.toList());

        return rootCategories.stream()
                .map(c -> convertToDto(c, allCategories))
                .collect(Collectors.toList());
    }

    private CategoryDto convertToDto(Category category, List<Category> allCategories) {
        List<CategoryDto> children = allCategories.stream()
                .filter(c -> category.getSeq().equals(c.getParentSeq()))
                .map(c -> convertToDto(c, allCategories))
                .collect(Collectors.toList());

        return CategoryDto.builder()
                .seq(category.getSeq())
                .categoryName(category.getCategoryName())
                .depthLevel(category.getDepthLevel())
                .parentSeq(category.getParentSeq())
                .children(children)
                .build();
    }
}
