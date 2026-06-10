package com.example.java.purchaseorder.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.purchaseorder.dto.InventoryListDTO;
import com.example.java.purchaseorder.dto.InventorySearchDTO;
import com.example.java.purchaseorder.service.InventoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class InventoryApiController {

    private final InventoryService inventoryService;

    @GetMapping("/inventories")
    public Slice<InventoryListDTO> loadMore(
            @ModelAttribute InventorySearchDTO search,
            @RequestParam("page") int page
    ) {

        Slice<InventoryListDTO> result = inventoryService.getListWithCond(
                search,
                PageRequest.of(page, 20)
        );
        
        System.out.println(
                "page=" + page +
                ", size=" + result.getContent().size() +
                ", hasNext=" + result.hasNext()
        );

        return result;
    }
}