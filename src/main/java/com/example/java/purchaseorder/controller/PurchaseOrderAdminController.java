package com.example.java.purchaseorder.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.product.entity.Options;
import com.example.java.product.service.OptionsService;
import com.example.java.purchaseorder.dto.PurchaseOrderListDTO;
import com.example.java.purchaseorder.entity.PurchaseOrder;
import com.example.java.purchaseorder.service.PurchaseOrderService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class PurchaseOrderAdminController {

	private final PurchaseOrderService purchaseOrderService;
	// TODO OptionsService 생기면 수정해야 함
	private final OptionsService optionsService;
	
	// TODO 임시 목록화면 (지금은 발주등록 이동위해 옵션 정보나오게 함, 나중에는 발주등록은 재고현황목록에서)
	@GetMapping("/purchase-orders")
	public String list(Model model) {

		List<PurchaseOrder> orders = purchaseOrderService.findAll();
		List<PurchaseOrderListDTO> list = orders.stream()
		        .map(PurchaseOrderListDTO::from)
		        .toList();

	    model.addAttribute("list", list);

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