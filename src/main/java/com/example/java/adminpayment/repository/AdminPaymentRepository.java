package com.example.java.adminpayment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.adminpayment.entity.AdminPayment;

public interface AdminPaymentRepository extends JpaRepository<AdminPayment, Long> {

	List<AdminPayment> findAllByOrderBySeqDesc();
}
