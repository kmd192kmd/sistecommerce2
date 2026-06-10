package com.example.java.stockhistory.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.entity.Options;
import com.example.java.stockhistory.dto.StockHistoryListDTO;
import com.example.java.stockhistory.dto.StockHistorySearchDTO;
import com.example.java.stockhistory.entity.StockHistory;
import com.example.java.stockhistory.enums.StockHistorySourceType;
import com.example.java.stockhistory.enums.StockHistoryType;
import com.example.java.stockhistory.repository.StockHistoryQueryDslRepository;
import com.example.java.stockhistory.repository.StockHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockHistoryService {

	private final StockHistoryRepository stockHistoryRepository;
	private final StockHistoryQueryDslRepository queryDslRepository;
	
	public List<StockHistory> getList() {
		return stockHistoryRepository.findAll(); 
	}
	
	@Transactional(readOnly = true)
	public Slice<StockHistoryListDTO> getListWithCond(StockHistorySearchDTO search, Pageable pageable) {
		
		List<StockHistory> contents =
	            queryDslRepository
	                    .findAllWithSearchCond(search, pageable);

	    boolean hasNext =
	            contents.size() > pageable.getPageSize();

	    if (hasNext) {
	        contents.remove(contents.size() - 1);
	    }

	    List<StockHistoryListDTO> dtoList =
	            contents.stream()
	                    .map(StockHistoryListDTO::from)
	                    .toList();

	    return new SliceImpl<>(
	            dtoList,
	            pageable,
	            hasNext
	    );
	}
	
	
	@Transactional
	public void createInStockHistory(Options options, int quantity,
									StockHistorySourceType sourceType, String reason) {
		
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
	public void createOutStockHistory(Options options, int quantity,
										StockHistorySourceType sourceType, String reason) {
		
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
