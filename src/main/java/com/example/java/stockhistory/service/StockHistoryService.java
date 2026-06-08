package com.example.java.stockhistory.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.entity.Options;
import com.example.java.stockhistory.entity.StockHistory;
import com.example.java.stockhistory.enums.StockHistorySourceType;
import com.example.java.stockhistory.enums.StockHistoryType;
import com.example.java.stockhistory.repository.StockHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockHistoryService {

	private final StockHistoryRepository stockHistoryRepository;
	
//	public List<StockHistoryListDTO>
	
	@Transactional
	public void createInStockHistory(Options options, int quantity, StockHistorySourceType sourceType, String reason) {
	    int before = options.getStock();
	    int after = before + quantity;

	    StockHistory history = StockHistory.builder()
	            .type(StockHistoryType.IN)
	            .reason(reason)
	            .quantity(quantity)
	            .beforeStock(before)
	            .afterStock(after)
	            .sourceType(sourceType)
	            .options(options)
	            .createdAt(LocalDateTime.now())
	            .build();

	    stockHistoryRepository.save(history);
	}
	
	@Transactional
	public void createOutStockHistory(Options options, int quantity, StockHistorySourceType sourceType, String reason) {
	    int before = options.getStock();
	    int after = before - quantity;

	    StockHistory history = StockHistory.builder()
	            .type(StockHistoryType.OUT)
	            .reason(reason)
	            .quantity(quantity)
	            .beforeStock(before)
	            .afterStock(after)
	            .sourceType(sourceType)
	            .options(options)
	            .createdAt(LocalDateTime.now())
	            .build();

	    stockHistoryRepository.save(history);
	}
}
