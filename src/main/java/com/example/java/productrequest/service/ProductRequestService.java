package com.example.java.productrequest.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.admin.entity.Admin;
import com.example.java.admin.repository.AdminRepository;
import com.example.java.productrequest.entity.ProductRequest;
import com.example.java.productrequest.repository.ProductRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductRequestService {

    private final ProductRequestRepository productRequestRepository;
    private final AdminRepository adminRepository;

    @Transactional
    public void approve(Long requestSeq, Long adminSeq) {
        ProductRequest request = productRequestRepository.findById(requestSeq)
                .orElseThrow(() -> new IllegalArgumentException("상품 요청이 존재하지 않습니다."));
        
        // TODO 임시로 adminSeq 1L 로 고정
        Admin admin = adminRepository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("관리자가 존재하지 않습니다."));

        request.approve(admin);
        request.getProduct().approve();
    }
    
    @Transactional
    public void approveAll(
            List<Long> requestSeqList,
            Long adminSeq) {

    	// TODO 임시로 adminSeq 1L 로 고정
        Admin admin = adminRepository.findById(1L)
                .orElseThrow();

        for (Long requestSeq : requestSeqList) {

            ProductRequest request = productRequestRepository.findById(requestSeq)
                    .orElseThrow();

            request.approve(admin);
        }
    }

    @Transactional
    public void reject(Long requestSeq, Long adminSeq, String rejectReason) {
        ProductRequest request = productRequestRepository.findById(requestSeq)
                .orElseThrow(() -> new IllegalArgumentException("상품 요청이 존재하지 않습니다."));
        
        // TODO 임시로 adminSeq 1L 로 고정
        Admin admin = adminRepository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("관리자가 존재하지 않습니다."));
        
        request.reject(admin, rejectReason);
        request.getProduct().reject();
    }
    
    @Transactional
    public void rejectAll(
            List<Long> requestSeqList,
            Long adminSeq,
            String rejectReason) {

    	// TODO 임시로 adminSeq 1L 로 고정
        Admin admin = adminRepository.findById(1L)
                .orElseThrow();

        for (Long requestSeq : requestSeqList) {

            ProductRequest request = productRequestRepository.findById(requestSeq)
                    .orElseThrow();

            request.reject(admin, rejectReason);
        }
    }
}