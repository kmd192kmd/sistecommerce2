package com.example.java.productrequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.productrequest.entity.ProductRequest;

public interface ProductRequestRepository extends JpaRepository<ProductRequest, Long> {

}
