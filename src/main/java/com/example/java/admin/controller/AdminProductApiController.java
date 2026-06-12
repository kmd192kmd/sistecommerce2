package com.example.java.admin.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.admin.dto.AdminProductListDto;
import com.example.java.admin.dto.AdminProductSearchDto;
import com.example.java.admin.service.AdminProductService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductApiController {

    private final AdminProductService adminProductService;

    @GetMapping
    public Slice<AdminProductListDto> getList(
            AdminProductSearchDto search,
            @PageableDefault(size = 10) Pageable pageable) {
        
        return adminProductService.getProductList(search, pageable);
    }

    @Data
    public static class BulkUpdateStatusRequest {
        private List<Long> productSeqs;
        private String status;
    }

    @Data
    public static class BulkUpdateSaleStatusRequest {
        private List<Long> productSeqs;
        private String saleStatus;
    }

    @PutMapping("/status")
    public ResponseEntity<?> updateStatus(@RequestBody BulkUpdateStatusRequest request) {
        adminProductService.updateProductStatus(request.getProductSeqs(), request.getStatus());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/sale-status")
    public ResponseEntity<?> updateSaleStatus(@RequestBody BulkUpdateSaleStatusRequest request) {
        adminProductService.updateProductSaleStatus(request.getProductSeqs(), request.getSaleStatus());
        return ResponseEntity.ok().build();
    }
}
