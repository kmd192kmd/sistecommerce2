package com.example.java.admin.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.admin.dto.AdminProductListDto;
import com.example.java.admin.dto.AdminProductSearchDto;
import com.example.java.admin.repository.AdminProductQueryRepository;
import com.example.java.admin.repository.AdminProductRepository;
import com.example.java.product.entity.Product;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final AdminProductQueryRepository adminProductQueryRepository;
    private final AdminProductRepository adminProductRepository;

    public Slice<AdminProductListDto> getProductList(AdminProductSearchDto searchDto, Pageable pageable) {
        return adminProductQueryRepository.findProducts(searchDto, pageable);
    }

    @Transactional
    public void updateProductStatus(List<Long> productSeqs, String status) {
        List<Product> products = adminProductRepository.findAllById(productSeqs);
        for (Product product : products) {
            product.updateStatus(status);
            if ("DELETED".equals(status)) {
                product.updateSaleStatus("STOPPED");
            }
        }
    }

    @Transactional
    public void updateProductSaleStatus(List<Long> productSeqs, String saleStatus) {
        List<Product> products = adminProductRepository.findAllById(productSeqs);
        for (Product product : products) {
            product.updateSaleStatus(saleStatus);
        }
    }
}
