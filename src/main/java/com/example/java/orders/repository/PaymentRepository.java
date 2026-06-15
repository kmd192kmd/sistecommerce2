package com.example.java.orders.repository;

import com.example.java.orders.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByExternalPaymentId(String externalPaymentId);

    /*
        주문의 결제완료 payment row를 조회한다.
        status = 2 : 결제완료
     */
    Optional<Payment> findTopByOrderSeqAndStatusOrderBySeqDesc(Long orderSeq, Integer status);
}