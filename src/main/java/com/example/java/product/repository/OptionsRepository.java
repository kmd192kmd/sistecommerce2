package com.example.java.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.product.entity.Options;

public interface OptionsRepository extends JpaRepository<Options, Long> {
    List<Options> findByProductSeq(Long productSeq);
}
