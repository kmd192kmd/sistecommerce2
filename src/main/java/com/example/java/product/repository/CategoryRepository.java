package com.example.java.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.product.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByDepthLevel(Integer depthLevel);
    List<Category> findByParentSeq(Long parentSeq);
}
