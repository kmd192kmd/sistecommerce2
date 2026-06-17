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
	private final com.example.java.product.repository.ProductDetailRepository productDetailRepository;
	
	public Options findById(Long optionSeq) {
		return optionsRepository.findById(optionSeq)
	            .orElseThrow(() -> new IllegalArgumentException("옵션 없음"));
	}
	
	@Transactional
	public void increaseStock(Long optionSeq, int quantity) {

	    Options option = optionsRepository.findById(optionSeq)
	            .orElseThrow(() -> new IllegalArgumentException("옵션 없음"));

	    option.increaseStock(quantity);
	    if (option.getProduct() != null) {
	        updateProductSaleStatus(option.getProduct().getSeq());
	    }
	}
	
	@Transactional
	public void decreaseStock(Long optionSeq, int quantity) {

	    Options option = optionsRepository.findById(optionSeq)
	            .orElseThrow(() -> new IllegalArgumentException("옵션 없음"));

	    option.decreaseStock(quantity);
	    if (option.getProduct() != null) {
	        updateProductSaleStatus(option.getProduct().getSeq());
	    }
	}

	@Transactional
	public void updateProductSaleStatus(Long productSeq) {
		if (productSeq == null) return;
		Integer totalStock = optionsRepository.sumStockByProductSeq(productSeq);
		if (totalStock == null) return;

		productDetailRepository.findById(productSeq).ifPresent(product -> {
			String currentStatus = product.getSaleStatus();
			String newStatus = (totalStock <= 0) ? "SOLD_OUT" : "ON_SALE";
			if (!newStatus.equals(currentStatus)) {
				product.setSaleStatus(newStatus);
				productDetailRepository.save(product);
			}
		});
	}
}
