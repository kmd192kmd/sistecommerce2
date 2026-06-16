package com.example.java.delivery.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.java.delivery.entity.Delivery;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long>{


	List<Delivery> findByOrders_Seq(Long orderSeq);
	
	@Query("SELECT d FROM Delivery d WHERE d.orders.memberSeq = :memberSeq ORDER BY d.dispatch_at DESC")
    List<Delivery> findTop1ByOrders_MemberSeqOrderByDispatchAtDesc(@Param("memberSeq") Long memberSeq, Pageable pageable);
	
	@Query("SELECT d FROM Delivery d JOIN FETCH d.deliveryCompany WHERE d.orders.seq = :orderSeq")
	List<Delivery> findByOrdersSeqWithCompany(@Param("orderSeq") Long orderSeq);
	
	
}
