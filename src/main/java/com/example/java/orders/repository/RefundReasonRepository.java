package com.example.java.orders.repository;

import com.example.java.orders.entity.RefundReason;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefundReasonRepository extends JpaRepository<RefundReason, Long> {
    Optional<RefundReason> findByReason(String reason);
}
