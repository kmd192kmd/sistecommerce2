package com.example.java.product.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    // Map을 활용하여 자식 카테고리를 O(1)로 조회하는 하이브리드 재귀 방식으로 성능 최적화 (O(N) 시간 복잡도)
    public List<CategoryDto> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();

        // 부모 시퀀스(parentSeq)별로 자식 리스트를 미리 그룹화 (Map 생성)
        Map<Long, List<Category>> childrenMap = allCategories.stream()
                .filter(c -> c.getParentSeq() != null)
                .collect(Collectors.groupingBy(Category::getParentSeq));

        List<Category> rootCategories = allCategories.stream()
                .filter(c -> c.getDepthLevel() == 0)
                .collect(Collectors.toList());

        return rootCategories.stream()
                .map(c -> convertToDto(c, childrenMap))
                .collect(Collectors.toList());
    }

    private CategoryDto convertToDto(Category category, Map<Long, List<Category>> childrenMap) {
        List<Category> childrenList = childrenMap.getOrDefault(category.getSeq(), Collections.emptyList());

        List<CategoryDto> children = childrenList.stream()
                .map(c -> convertToDto(c, childrenMap))
                .collect(Collectors.toList());

        return CategoryDto.builder()
                .seq(category.getSeq())
                .categoryName(category.getCategoryName())
                .depthLevel(category.getDepthLevel())
                .parentSeq(category.getParentSeq())
                .children(children)
                .build();
    }

    // 특정 카테고리 ID를 기점으로 모든 하위 카테고리(소분류 포함) ID 목록을 반환
    public List<Long> getDescendantCategorySeqs(Long categorySeq) {
        if (categorySeq == null) {
            return null;
        }
        List<Category> allCategories = categoryRepository.findAll();

        List<Long> seqs = new ArrayList<>();
        seqs.add(categorySeq);

        // 1단계 하위 카테고리 탐색 (중분류)
        List<Long> children1 = allCategories.stream()
                .filter(c -> categorySeq.equals(c.getParentSeq()))
                .map(Category::getSeq)
                .collect(Collectors.toList());
        seqs.addAll(children1);

        // 2단계 하위 카테고리 탐색 (소분류)
        if (!children1.isEmpty()) {
            List<Long> children2 = allCategories.stream()
                    .filter(c -> c.getParentSeq() != null && children1.contains(c.getParentSeq()))
                    .map(Category::getSeq)
                    .collect(Collectors.toList());
            seqs.addAll(children2);
        }
        return seqs;
    }

    // 특정 카테고리 ID로부터 루트 대분류까지의 경로를 반환
    public List<CategoryDto> getCategoryPath(Long categorySeq) {
        List<CategoryDto> path = new java.util.ArrayList<>();
        if (categorySeq == null) {
            return path;
        }
        List<Category> allCategories = categoryRepository.findAll();
        Category current = allCategories.stream()
                .filter(c -> c.getSeq().equals(categorySeq))
                .findFirst()
                .orElse(null);

        while (current != null) {
            path.add(0, CategoryDto.builder()
                    .seq(current.getSeq())
                    .categoryName(current.getCategoryName())
                    .depthLevel(current.getDepthLevel())
                    .parentSeq(current.getParentSeq())
                    .build());
            Long parentSeq = current.getParentSeq();
            if (parentSeq == null) {
                break;
            }
            current = allCategories.stream()
                    .filter(c -> c.getSeq().equals(parentSeq))
                    .findFirst()
                    .orElse(null);
        }
        return path;
    }
}
