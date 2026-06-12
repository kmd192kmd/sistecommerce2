package com.example.java.orders.repository;

import com.example.java.orders.entity.Returns;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReturnsRepository extends JpaRepository<Returns, Long> {
    List<Returns> findByStatus(String status);
}
