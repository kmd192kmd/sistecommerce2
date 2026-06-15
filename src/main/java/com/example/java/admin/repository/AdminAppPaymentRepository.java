package com.example.java.admin.repository;

import com.example.java.orders.entity.Payment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAppPaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByStatus(Integer status);

    // 고객 결제 완료 건들의 총합 (쇼핑몰 총 매출)
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status")
    Long sumAmountByStatus(@Param("status") Integer status);
}
