package com.example.java.product.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.dto.ProductManageDto;
import com.example.java.product.repository.ProductManageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductManageService {

    private final ProductManageRepository productManageRepository;


    /*
        상품 관리 화면 목록을 조회합니다.

        Controller에서 받은 검색 조건과 페이징 정보를 Repository로 전달합니다.

        이 메서드는 관리자 상품 요청 목록에서 사용합니다.
        전체 판매처의 상품 요청을 조회해야 하므로 sellerSeq를 받지 않습니다.
    */
    public List<ProductManageDto> getProductRequests(String approvalStatus,
                                                     String startDate,
                                                     String endDate,
                                                     String searchType,
                                                     String keyword,
                                                     String sortType,
                                                     int page,
                                                     int size) {

        int offset = (page - 1) * size;

        return productManageRepository.findProductRequests(
                approvalStatus,
                startDate,
                endDate,
                searchType,
                keyword,
                sortType,
                offset,
                size
        );
    }


    /*
        상품 관리 화면 검색 결과 전체 개수를 조회합니다.

        이 값으로 페이지네이션을 계산합니다.

        이 메서드는 관리자 상품 요청 목록에서 사용합니다.
        전체 판매처의 상품 요청 개수를 조회해야 하므로 sellerSeq를 받지 않습니다.
    */
    public int countProductRequests(String approvalStatus,
                                    String startDate,
                                    String endDate,
                                    String searchType,
                                    String keyword) {

        return productManageRepository.countProductRequests(
                approvalStatus,
                startDate,
                endDate,
                searchType,
                keyword
        );
    }


    /*
        선택한 상품을 삭제 처리합니다.

        실제 DB DELETE가 아니라 product.status를 DELETED로 변경합니다.

        이 메서드는 기존 관리자용 삭제 로직입니다.
        기존 기능을 깨지 않기 위해 sellerSeq를 받지 않습니다.
    */
    @Transactional
    public void deleteProducts(List<Long> productSeqs) {

        if (productSeqs == null || productSeqs.isEmpty()) {
            throw new IllegalArgumentException("삭제할 상품을 선택해주세요.");
        }

        productManageRepository.deleteProducts(productSeqs);
    }


    /*
        판매처 상품 관리 화면 목록을 조회합니다.

        관리자 상품 요청 목록과 달리
        현재 로그인한 판매처의 sellerSeq 조건을 추가해서 조회합니다.
    */
    public List<ProductManageDto> getSellerProductRequests(Long sellerSeq,
                                                           String approvalStatus,
                                                           String startDate,
                                                           String endDate,
                                                           String searchType,
                                                           String keyword,
                                                           String sortType,
                                                           int page,
                                                           int size) {

        int offset = (page - 1) * size;

        return productManageRepository.findSellerProductRequests(
                sellerSeq,
                approvalStatus,
                startDate,
                endDate,
                searchType,
                keyword,
                sortType,
                offset,
                size
        );
    }


    /*
        판매처 상품 관리 화면 검색 결과 전체 개수를 조회합니다.

        현재 로그인한 판매처의 sellerSeq 조건을 추가해서
        해당 판매처 상품 요청 개수만 조회합니다.
    */
    public int countSellerProductRequests(Long sellerSeq,
                                          String approvalStatus,
                                          String startDate,
                                          String endDate,
                                          String searchType,
                                          String keyword) {

        return productManageRepository.countSellerProductRequests(
                sellerSeq,
                approvalStatus,
                startDate,
                endDate,
                searchType,
                keyword
        );
    }


    /*
        판매처 상품 관리 화면에서 선택한 상품을 삭제 처리합니다.

        현재 로그인한 판매처의 상품만 삭제할 수 있도록
        sellerSeq 조건을 함께 전달합니다.
    */
    @Transactional
    public void deleteSellerProducts(Long sellerSeq, List<Long> productSeqs) {

        if (productSeqs == null || productSeqs.isEmpty()) {
            throw new IllegalArgumentException("삭제할 상품을 선택해주세요.");
        }

        productManageRepository.deleteSellerProducts(sellerSeq, productSeqs);
    }
}