package com.example.java.cart.repository;

import com.example.java.cart.entity.CartLog;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;


public interface CartLogRepository extends JpaRepository<CartLog, Long> {
    @Query("SELECT c FROM CartLog c LEFT JOIN FETCH c.options o LEFT JOIN FETCH o.product p LEFT JOIN FETCH c.member m WHERE c.member.seq = :memberSeq ORDER BY c.actionDate DESC")
    List<CartLog> findTop10ByMember_SeqOrderByActionDateDesc(@Param("memberSeq") Long memberSeq);

    @Query("SELECT c FROM CartLog c LEFT JOIN FETCH c.options o LEFT JOIN FETCH o.product p LEFT JOIN FETCH c.member m")
    List<CartLog> findAllWithDetails();
}
