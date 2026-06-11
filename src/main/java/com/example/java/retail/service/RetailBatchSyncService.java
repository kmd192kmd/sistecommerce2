package com.example.java.retail.service;

import com.example.java.cart.entity.CartLog;
import com.example.java.cart.repository.CartLogRepository;
import com.example.java.product.dto.ProductDto;
import com.example.java.product.repository.ProductRepository;
import com.google.cloud.retail.v2.ProductServiceClient;
import com.google.cloud.retail.v2.UpdateProductRequest;
import com.google.cloud.retail.v2.UserEventServiceClient;
import com.google.cloud.retail.v2.UserEvent;
import com.google.cloud.retail.v2.ProductDetail;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetailBatchSyncService {

    private final ProductRepository productRepository;
    private final CartLogRepository cartLogRepository;
    private final ProductServiceClient productServiceClient;
    private final UserEventServiceClient userEventServiceClient;

    @Value("${google.cloud.project-id}")
    private String projectId;

    @Value("${retail.location:global}")
    private String location;

    @Value("${retail.catalog:default_catalog}")
    private String catalog;
    
    @Value("${retail.branch:default_branch}")
    private String branch;

    /**
     * DB의 기존 상품 전체를 구글 Retail AI 카탈로그로 일괄 업로드합니다.
     */
    @Async
    @Transactional(readOnly = true)
    public void syncAllProducts() {
        log.info("--- 기존 상품 데이터 전체 일괄 동기화 시작 ---");
        int totalProducts = productRepository.countProducts();
        int pageSize = 100;
        int successCount = 0;
        int failCount = 0;
        
        String parent = String.format("projects/%s/locations/%s/catalogs/%s/branches/%s", 
                                      projectId, location, catalog, branch);

        for (int offset = 0; offset < totalProducts; offset += pageSize) {
            List<ProductDto> products = productRepository.findProductsByPaging(offset, pageSize);
            
            for (ProductDto dto : products) {
                try {
                    String name = parent + "/products/" + dto.getSeq();
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

                    UpdateProductRequest updateRequest = UpdateProductRequest.newBuilder()
                            .setProduct(retailProduct)
                            .setAllowMissing(true) 
                            .build();
                    
                    productServiceClient.updateProduct(updateRequest);
                    successCount++;
                    
                    // 구글 API Rate Limit (초당 요청 제한) 방지 및 커넥션 안정성을 위한 0.05초 대기
                    Thread.sleep(50); 
                } catch (Exception e) {
                    failCount++;
                    log.error("상품 일괄 동기화 실패 (상품 SEQ: {}): {}", dto.getSeq(), e.getMessage());
                }
            }
        }
        log.info("--- 상품 일괄 동기화 완료 (성공: {}, 실패: {}) ---", successCount, failCount);
    }

    /**
     * 기존 장바구니/구매(cart_log) 기록 전체를 구글 Retail AI 사용자 이벤트로 일괄 업로드합니다.
     */
    @Async
    @Transactional(readOnly = true)
    public void syncAllCartLogs() {
        log.info("--- 기존 장바구니/구매 기록 일괄 동기화 시작 ---");
        List<CartLog> logs = cartLogRepository.findAllWithDetails();
        int successCount = 0;
        int failCount = 0;

        String parent = String.format("projects/%s/locations/%s/catalogs/%s", projectId, location, catalog);

        for (CartLog logData : logs) {
            try {
                if (logData.getOptions() == null || logData.getOptions().getProduct() == null) continue;
                
                String eventType = "detail-page-view";
                if (CartLog.STATUS_ADD.equals(logData.getStatus())) eventType = "add-to-cart";
                else if (CartLog.STATUS_PURCHASE.equals(logData.getStatus())) eventType = "purchase-complete";
                else if (CartLog.STATUS_REMOVE.equals(logData.getStatus())) eventType = "remove-from-cart";

                long epochSeconds = logData.getActionDate().toEpochSecond(ZoneOffset.UTC);

                UserEvent.Builder userEventBuilder = UserEvent.newBuilder()
                        .setEventType(eventType)
                        .setVisitorId(logData.getMember() != null ? String.valueOf(logData.getMember().getSeq()) : "guest-visitor")
                        .setEventTime(Timestamp.newBuilder().setSeconds(epochSeconds).build())
                        .addProductDetails(ProductDetail.newBuilder()
                                .setProduct(com.google.cloud.retail.v2.Product.newBuilder()
                                        .setId(String.valueOf(logData.getOptions().getProduct().getSeq()))
                                        .build())
                                .setQuantity(com.google.protobuf.Int32Value.newBuilder().setValue(1).build())
                                .build());

                if ("purchase-complete".equals(eventType)) {
                    userEventBuilder.setPurchaseTransaction(com.google.cloud.retail.v2.PurchaseTransaction.newBuilder()
                            .setId(String.valueOf(logData.getSeq()))
                            .setRevenue(100.0f) // 임의의 값 설정
                            .setCurrencyCode("KRW")
                            .build());
                }

                UserEvent userEvent = userEventBuilder.build();

                userEventServiceClient.writeUserEvent(com.google.cloud.retail.v2.WriteUserEventRequest.newBuilder()
                        .setParent(parent)
                        .setUserEvent(userEvent)
                        .build());
                successCount++;
                
                // 구글 API Rate Limit 방지
                Thread.sleep(50);
            } catch (Exception e) {
                failCount++;
                log.error("사용자 이벤트 일괄 동기화 실패 (Log Seq: {}): {}", logData.getSeq(), e.getMessage());
            }
        }
        log.info("--- 사용자 이벤트 일괄 동기화 완료 (성공: {}, 실패: {}) ---", successCount, failCount);
    }
}
