package com.example.java.delivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.delivery.entity.Delivery;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long>{

	List<Delivery> findByOrders_Seq(Long orderSeq);
	
}
