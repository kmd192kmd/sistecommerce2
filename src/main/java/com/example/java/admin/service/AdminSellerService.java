package com.example.java.admin.service;

import com.example.java.admin.dto.AdminSellerProductDto;
import com.example.java.admin.repository.AdminOptionsRepository;
import com.example.java.admin.repository.AdminProductRepository;
import com.example.java.admin.repository.AdminSellerRepository;
import com.example.java.product.entity.Options;
import com.example.java.product.entity.Product;
import com.example.java.product.entity.Seller;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSellerService {

    private final AdminSellerRepository adminSellerRepository;
    private final AdminProductRepository adminProductRepository;
    private final AdminOptionsRepository adminOptionsRepository;

    // 판매처 목록 조회 (이름 검색 지원)
    public List<Seller> searchSellers(String name) {
        if (name == null || name.trim().isEmpty()) {
            return adminSellerRepository.findAll();
        }
        return adminSellerRepository.findByNameContaining(name.trim());
    }

    // 특정 판매처의 상품 리스트 및 총 재고량 조회 (상품명 검색 지원)
    public List<AdminSellerProductDto> getSellerProducts(Long sellerSeq, String productName) {
        List<Product> products;
        if (productName == null || productName.trim().isEmpty()) {
            products = adminProductRepository.findBySellerSeq(sellerSeq);
        } else {
            products = adminProductRepository.findBySellerSeqAndProductNameContaining(sellerSeq, productName.trim());
        }

        return products.stream().map(product -> {
            List<Options> options = adminOptionsRepository.findByProductSeq(product.getSeq());
            int totalStock = options.stream().mapToInt(Options::getStock).sum();
            return AdminSellerProductDto.builder()
                    .product(product)
                    .totalStock(totalStock)
                    .build();
        }).collect(Collectors.toList());
    }

    // 상품 상태(NORMAL/DELETED 등) 변경
    @Transactional
    public void changeProductStatus(Long productSeq, String status) {
        Product product = adminProductRepository.findById(productSeq)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. seq=" + productSeq));
        product.setStatus(status);
    }
}
