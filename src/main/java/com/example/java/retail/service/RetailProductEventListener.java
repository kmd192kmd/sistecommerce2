package com.example.java.retail.service;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.event.ProductUpdatedEvent;
import com.example.java.product.repository.ProductDetailRepository;
import com.google.cloud.retail.v2.ProductServiceClient;
import com.google.cloud.retail.v2.UpdateProductRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetailProductEventListener {

    private final ProductServiceClient productServiceClient;
    private final ProductDetailRepository productDetailRepository;

    @Value("${google.cloud.project-id}")
    private String projectId;

    @Value("${retail.location:global}")
    private String location;

    @Value("${retail.catalog:default_catalog}")
    private String catalog;
    
    @Value("${retail.branch:default_branch}")
    private String branch;

    /**
     * DB에서 상품이 생성/수정/삭제되어 알림이 울릴 때마다 구글 Retail AI 카탈로그로 전송합니다.
     */
    @Async
    @EventListener
    public void handleProductUpdatedEvent(ProductUpdatedEvent event) {
        Long productSeq = event.getProductSeq();
        try {
            productDetailRepository.findProductDetail(productSeq).ifPresentOrElse(
                dto -> syncProductToGoogle(dto),
                () -> deleteProductFromGoogle(productSeq) // 만약 DB에 없으면(완전삭제 등) 구글에서도 삭제
            );
        } catch (Exception e) {
            log.error("Retail AI 자동 상품 동기화 실패 (상품 ID: {})", productSeq, e);
        }
    }

    private void syncProductToGoogle(ProductDto dto) {
        String parent = String.format("projects/%s/locations/%s/catalogs/%s/branches/%s", 
                                      projectId, location, catalog, branch);

        String name = parent + "/products/" + dto.getSeq();

        // 1. Retail AI 규격에 맞게 변환
        com.google.cloud.retail.v2.Product retailProduct = com.google.cloud.retail.v2.Product.newBuilder()
                .setName(name)
                .setId(String.valueOf(dto.getSeq()))
                .setTitle(dto.getProductName() != null ? dto.getProductName() : "이름 없음")
                .setUri(dto.getThumbnailUrl() != null ? dto.getThumbnailUrl() : "")
                .setPriceInfo(com.google.cloud.retail.v2.PriceInfo.newBuilder()
                        .setPrice((float) dto.getPrice())
                        .setCurrencyCode("KRW")
                        .build())
                .addCategories(dto.getCategorySeq() != null ? String.valueOf(dto.getCategorySeq()) : "Default")
                .setAvailability("N".equals(dto.getHideYn()) 
                        ? com.google.cloud.retail.v2.Product.Availability.IN_STOCK 
                        : com.google.cloud.retail.v2.Product.Availability.OUT_OF_STOCK)
                .build();

        // 2. 구글로 업데이트 (allowMissing=true이므로 없으면 자동으로 신규 생성됨)
        try {
             UpdateProductRequest updateRequest = UpdateProductRequest.newBuilder()
                     .setProduct(retailProduct)
                     .setAllowMissing(true) 
                     .build();
             
             productServiceClient.updateProduct(updateRequest);
             log.info("Retail AI 자동 상품 동기화(업데이트/생성) 완료: {}", dto.getSeq());
        } catch (Exception ex) {
             log.error("Retail AI 자동 상품 동기화 실패: {}", dto.getSeq(), ex);
        }
    }
    
    private void deleteProductFromGoogle(Long productSeq) {
        String name = String.format("projects/%s/locations/%s/catalogs/%s/branches/%s/products/%s", 
                                      projectId, location, catalog, branch, productSeq);
        try {
            productServiceClient.deleteProduct(com.google.cloud.retail.v2.DeleteProductRequest.newBuilder()
                    .setName(name)
                    .build());
            log.info("Retail AI 자동 상품 동기화(삭제) 완료: {}", productSeq);
        } catch (Exception e) {
            log.error("Retail AI 자동 상품 삭제 실패 (이미 없는 상품일 수 있음): {}", productSeq);
        }
    }
}
