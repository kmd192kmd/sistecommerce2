package com.example.java.delivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.delivery.entity.Delivery;

public interface DeliveryRepository extends JpaRepository<Delivery, Long>{

}
