package com.example.java.productrequest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.productrequest.dto.ProductRequestProcessDto;
import com.example.java.productrequest.service.ProductRequestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProductRequestApiController {

    private final ProductRequestService productRequestService;

    @PostMapping("/product-requests/approve")
    public ResponseEntity<?> approve(
            @RequestBody ProductRequestProcessDto dto) {

        productRequestService.approveAll(dto.getRequestSeqList(), dto.getAdminSeq());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/product-requests/reject")
    public ResponseEntity<?> reject(
            @RequestBody ProductRequestProcessDto dto) {

        productRequestService.rejectAll(
                dto.getRequestSeqList(),
                dto.getAdminSeq(),
                dto.getRejectReason()
        );

        return ResponseEntity.ok().build();
    }
}