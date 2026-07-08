package com.example.java.orders.repository;

import com.example.java.orders.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    Optional<Orders> findByOrderUid(String orderUid);

//    /**
//     * 주문 상태 변경 및 취소 시 동시성 경쟁을 제어하기 위한 비관적 락 조회
//     */
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("select o from Orders o where o.seq = :seq")
//    Optional<Orders> findBySeqForUpdate(@Param("seq") Long seq);
    
    @Lock(LockModeType.OPTIMISTIC) // 또는 OPTIMISTIC_FORCE_INCREMENT
    Optional<Orders> findBySeq(Long seq);
}