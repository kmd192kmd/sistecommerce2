package com.example.java.admin.repository;

import com.example.java.adminpayment.entity.AdminPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminSellerPaymentRepository extends JpaRepository<AdminPayment, Long> {
    
    // 관리자가 판매처에 정산 완료한 발주(PurchaseOrder)의 총 대금 합산
    @Query("SELECT SUM(po.totalPrice) FROM AdminPayment ap JOIN ap.purchaseOrder po WHERE ap.status = :status")
    Long sumPayoutByStatus(@Param("status") Integer status);
}
