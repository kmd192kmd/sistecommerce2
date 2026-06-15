package com.example.java.orders.repository;

import com.example.java.orders.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByOrderItemSeq(Long orderItemSeq);
}