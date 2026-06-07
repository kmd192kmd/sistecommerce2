package com.example.java.purchaseorder.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.product.entity.Options;
import com.example.java.product.service.OptionsService;
import com.example.java.purchaseorder.dto.PurchaseOrderListDTO;
import com.example.java.purchaseorder.service.PurchaseOrderService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class PurchaseOrderAdminController {

	private final PurchaseOrderService purchaseOrderService;
	private final OptionsService optionsService;
	
	// TODO 목록화면 (재고현황목록에 발주등록 추가해야 함)
	@GetMapping("/purchase-orders")
	public String list(Model model) {

//		List<PurchaseOrderListDTO> list = purchaseOrderService.getList();
	    Slice<PurchaseOrderListDTO> slice =
	            purchaseOrderService.getList(
	                    PageRequest.of(0, 20)
	            );

	    model.addAttribute("list", slice.getContent());
	    model.addAttribute("hasNext", slice.hasNext());

	    return "admin/purchase-order/list";
	}
	
	@GetMapping("/purchase-orders/new/{optionsSeq}")
	public String showCreateForm(@PathVariable(value = "optionsSeq") Long optionsSeq, Model model) {

		// TODO null 오류처리 필요
		Options options = optionsService.findById(optionsSeq);
		
		// TODO Options 이 product랑 연결되면 수정해야 함
		int supplyPrice = 200;

	    model.addAttribute("options", options);
	    model.addAttribute("supplyPrice", supplyPrice);

	    return "admin/purchase-order/add";
	}
}