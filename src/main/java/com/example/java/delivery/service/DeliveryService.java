package com.example.java.delivery.service;

import org.springframework.stereotype.Service;

import com.example.java.delivery.repository.DeliveryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryService {

	private final DeliveryRepository deliveryRepository;
	
}
