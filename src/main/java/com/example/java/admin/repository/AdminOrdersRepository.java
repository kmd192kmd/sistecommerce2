package com.example.java.admin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.java.orders.entity.Orders;

@Repository
public interface AdminOrdersRepository extends JpaRepository<Orders, Long> {
    
    List<Orders> findTop5ByMemberSeqOrderByOrderDateDesc(Long memberSeq);
    
    Optional<Orders> findByOrderUid(String orderUid);

    Page<Orders> findAll(Pageable pageable);
    
    @Query("SELECT o FROM Orders o, Member m WHERE o.memberSeq = m.seq AND (" +
           "LOWER(m.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Orders> searchByMemberKeyword(@Param("keyword") String keyword, Pageable pageable);
}
