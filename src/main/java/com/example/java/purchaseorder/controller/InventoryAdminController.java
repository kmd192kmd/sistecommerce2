package com.example.java.purchaseorder.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.java.purchaseorder.dto.InventoryListDTO;
import com.example.java.purchaseorder.dto.InventorySearchDTO;
import com.example.java.purchaseorder.service.InventoryService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class InventoryAdminController {

	private final InventoryService inventoryService;
	
	@GetMapping("/inventories")
	public String list(
	        @ModelAttribute InventorySearchDTO search,
	        Model model
	) {

	    Slice<InventoryListDTO> slice =
	            inventoryService.getListWithCond(
	                    search,
	                    PageRequest.of(0, 20)
	            );

	    model.addAttribute("search", search);
	    model.addAttribute("list", slice.getContent());
	    model.addAttribute("hasNext", slice.hasNext());

	    return "admin/inventory/list";
	}
}
