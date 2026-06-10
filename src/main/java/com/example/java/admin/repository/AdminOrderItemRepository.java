package com.example.java.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.java.orders.entity.OrderItem;

@Repository
public interface AdminOrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrderSeq(Long orderSeq);

}
