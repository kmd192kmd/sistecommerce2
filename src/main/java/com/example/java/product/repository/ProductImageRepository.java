package com.example.java.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.product.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    Optional<ProductImage> findFirstByProductSeqAndThumbnailYnAndStatus(Long productSeq, String thumbnailYn, String status);
    List<ProductImage> findByProductSeqAndStatusOrderByImageOrderAsc(Long productSeq, String status);
}
