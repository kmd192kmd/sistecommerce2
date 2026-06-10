package com.example.java.admin.repository;

import com.example.java.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySellerSeq(Long sellerSeq);
    List<Product> findBySellerSeqAndProductNameContaining(Long sellerSeq, String productName);
}
