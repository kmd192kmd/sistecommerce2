package com.example.java.purchaseorder.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.product.service.OptionsService;
import com.example.java.purchaseorder.dto.PurchaseOrderListDTO;
import com.example.java.purchaseorder.dto.PurchaseOrderSearchDTO;
import com.example.java.purchaseorder.service.PurchaseOrderService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class PurchaseOrderAdminController {

	private final PurchaseOrderService purchaseOrderService;
	
	// TODO 목록화면 (재고현황목록에 발주등록 추가해야 함)
	@GetMapping("/purchase-orders")
	public String list(@ModelAttribute PurchaseOrderSearchDTO search, Model model) {

//		List<PurchaseOrderListDTO> list = purchaseOrderService.getList();
	    Slice<PurchaseOrderListDTO> slice =
	            purchaseOrderService.getList(
	            		search, PageRequest.of(0, 20)
	            );

	    model.addAttribute("search", search);
	    model.addAttribute("list", slice.getContent());
	    model.addAttribute("hasNext", slice.hasNext());

	    return "admin/purchase-order/list";
	}
	
	@GetMapping("/purchase-orders/new/{optionsSeq}")
	public String showCreateForm(@PathVariable(value = "optionsSeq") Long optionsSeq, Model model) {

		java.util.Map<String, Object> formData = purchaseOrderService.getCreateFormData(optionsSeq);
		model.addAllAttributes(formData);

	    return "admin/purchase-order/add";
	}
}