package com.example.java.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.entity.Options;
import com.example.java.product.repository.OptionsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptionsService {

	private final OptionsRepository optionsRepository;
	
	public Options findById(Long optionSeq) {
		return optionsRepository.findById(optionSeq)
	            .orElseThrow(() -> new IllegalArgumentException("옵션 없음"));
	}
	
	@Transactional
	public void receiveStock(Long optionSeq, int quantity) {

	    Options option = optionsRepository.findById(optionSeq)
	            .orElseThrow(() -> new IllegalArgumentException("옵션 없음"));

	    option.increaseStock(quantity);
	}
	
	@Transactional
	public void releaseStock(Long optionSeq, int quantity) {

	    Options option = optionsRepository.findById(optionSeq)
	            .orElseThrow(() -> new IllegalArgumentException("옵션 없음"));

	    option.decreaseStock(quantity);
	}
}
