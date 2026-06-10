package com.example.java.admin.repository;

import com.example.java.product.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminSellerRepository extends JpaRepository<Seller, Long> {
    List<Seller> findByNameContaining(String name);
}
