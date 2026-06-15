package com.example.java.orders.repository;

import com.example.java.orders.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderSeq(Long orderSeq);

    /** 공구 참여(participation_seq)로 주문상품 조회 — 공구 환불 시 원주문을 찾는 데 쓴다. */
    List<OrderItem> findByParticipationSeq(Long participationSeq);
}