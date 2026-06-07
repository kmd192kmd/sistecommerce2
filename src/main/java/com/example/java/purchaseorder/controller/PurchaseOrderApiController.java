package com.example.java.purchaseorder.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.purchaseorder.dto.PurchaseOrderCreateDTO;
import com.example.java.purchaseorder.dto.PurchaseOrderListDTO;
import com.example.java.purchaseorder.dto.PurchaseOrderUpdateDTO;
import com.example.java.purchaseorder.service.PurchaseOrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PurchaseOrderApiController {

	private final PurchaseOrderService purchaseOrderService;
	
	@GetMapping("/purchase-orders")
	public Slice<PurchaseOrderListDTO> loadMore(@RequestParam("page") int page) {

	    return purchaseOrderService.getList(
	            PageRequest.of(page, 20)
	    );
	}
	
	@PostMapping("/purchase-orders")
    public ResponseEntity<Long> create(@RequestBody PurchaseOrderCreateDTO dto) {
		
        Long seq = purchaseOrderService.create(dto);
        return ResponseEntity.ok(seq);
    }
	
	@PostMapping("/purchase-orders/status")
	public ResponseEntity<Void> updateStatus(@RequestBody PurchaseOrderUpdateDTO dto) {

	    purchaseOrderService.updateStatus(dto.getSeqs(), dto.getStatus());
	    return ResponseEntity.ok().build();
	}
}
