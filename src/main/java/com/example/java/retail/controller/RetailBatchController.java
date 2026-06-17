package com.example.java.retail.controller;

import com.example.java.retail.service.RetailBatchSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/retail-sync")
@RequiredArgsConstructor
public class RetailBatchController {

    private final RetailBatchSyncService retailBatchSyncService;

    @PostMapping("/products")
    public ResponseEntity<String> syncProducts() {
        retailBatchSyncService.syncAllProducts();
        return ResponseEntity.ok("기존 상품 데이터(카탈로그) 전체 일괄 동기화가 백그라운드에서 시작되었습니다.");
    }

    @PostMapping("/events")
    public ResponseEntity<String> syncEvents() {
        retailBatchSyncService.syncAllCartLogs();
        return ResponseEntity.ok("기존 사용자 장바구니/구매 기록 전체 일괄 동기화가 백그라운드에서 시작되었습니다.");
    }
}
