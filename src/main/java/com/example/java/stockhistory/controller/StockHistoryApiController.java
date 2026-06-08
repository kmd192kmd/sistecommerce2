package com.example.java.stockhistory.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.purchaseorder.dto.PurchaseOrderListDTO;
import com.example.java.purchaseorder.dto.PurchaseOrderSearchDTO;
import com.example.java.purchaseorder.service.PurchaseOrderService;
import com.example.java.stockhistory.dto.StockHistoryListDTO;
import com.example.java.stockhistory.dto.StockHistorySearchDTO;
import com.example.java.stockhistory.service.StockHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class StockHistoryApiController {

	private final StockHistoryService stockHistoryService;
	
	@GetMapping("/stock-histories")
	public Slice<StockHistoryListDTO> loadMore(
			@ModelAttribute StockHistorySearchDTO search,
			@RequestParam("page") int page) {

	    return stockHistoryService.getListWithCond(search, PageRequest.of(page, 20));
	}
	
}
