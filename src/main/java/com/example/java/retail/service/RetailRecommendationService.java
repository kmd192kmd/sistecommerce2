package com.example.java.retail.service;

import com.example.java.cart.entity.CartLog;
import com.example.java.cart.repository.CartLogRepository;
import com.example.java.product.dto.ProductDto;
import com.example.java.product.repository.ProductDetailRepository;
import com.google.cloud.retail.v2.PredictRequest;
import com.google.cloud.retail.v2.PredictResponse;
import com.google.cloud.retail.v2.PredictionServiceClient;
import com.google.cloud.retail.v2.ProductDetail;
import com.google.cloud.retail.v2.UserEvent;
import com.google.protobuf.Int32Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetailRecommendationService {

    private final PredictionServiceClient predictionServiceClient;
    private final CartLogRepository cartLogRepository;
    private final ProductDetailRepository productDetailRepository;

    @Value("${google.cloud.project-id}")
    private String projectId;

    @Value("${retail.location:global}")
    private String location;

    @Value("${retail.catalog:default_catalog}")
    private String catalog;
    
    // Serving config ID in Retail AI Console (e.g. recently_viewed_default)
    @Value("${retail.serving-config:recently_viewed_default}")
    private String servingConfig;

    /**
     * cart_log 데이터를 기반으로 사용자에게 상품을 추천합니다.
     * @param memberSeq 사용자 ID
     * @return 추천 상품 목록
     */
    public List<ProductDto> recommendProducts(Long memberSeq) {
        if (memberSeq == null) {
            log.info("로그인하지 않은 사용자이므로 기본 추천(최신 상품)을 제공합니다.");
            return productDetailRepository.findProductsByPaging(0, 4);
        }

        // 1. 사용자의 최근 장바구니/구매 로그 조회 (최대 10건)
        List<CartLog> recentLogs = cartLogRepository.findTop10ByMember_SeqOrderByActionDateDesc(memberSeq);
        
        if (recentLogs.isEmpty()) {
            log.info("사용자 {}의 최근 활동 로그가 없어 기본 추천(최신 상품)을 제공합니다.", memberSeq);
            return productDetailRepository.findProductsByPaging(0, 4);
        }

        // 2. UserEvent 구성 (Retail AI에 전달할 사용자의 현재 Context)
        UserEvent.Builder userEventBuilder = UserEvent.newBuilder()
                .setEventType("add-to-cart")
                .setVisitorId(String.valueOf(memberSeq));

        for (CartLog log : recentLogs) {
            if (log.getOptions() != null && log.getOptions().getProduct() != null) {
                String productId = String.valueOf(log.getOptions().getProduct().getSeq());
                
                ProductDetail productDetail = ProductDetail.newBuilder()
                        .setProduct(com.google.cloud.retail.v2.Product.newBuilder()
                                .setId(productId)
                                .build())
                        .setQuantity(Int32Value.newBuilder().setValue(1).build())
                        .build();
                        
                userEventBuilder.addProductDetails(productDetail);
            }
        }

        // 3. Predict 요청 생성
        String placement = String.format("projects/%s/locations/%s/catalogs/%s/servingConfigs/%s",
                projectId, location, catalog, servingConfig);

        PredictRequest request = PredictRequest.newBuilder()
                .setPlacement(placement)
                .setUserEvent(userEventBuilder.build())
                .build();

        List<ProductDto> recommendedProducts = new ArrayList<>();
        try {
            // Retail API 호출
            PredictResponse response = predictionServiceClient.predict(request);
            
            // 4. 추천된 상품 ID를 바탕으로 DB에서 상품 정보 조회
            for (PredictResponse.PredictionResult result : response.getResultsList()) {
                String recommendedProductId = result.getId();
                try {
                    Long prodSeq = Long.parseLong(recommendedProductId);
                    Optional<ProductDto> productOpt = productDetailRepository.findProductDetail(prodSeq);
                    productOpt.ifPresent(recommendedProducts::add);
                } catch (NumberFormatException e) {
                    log.warn("Retail AI로부터 받은 잘못된 상품 ID 포맷: {}", recommendedProductId);
                }
            }
        } catch (Exception e) {
            log.error("Google Cloud Retail API 추천 요청 실패", e);
        }

        // 5. 추천된 상품이 없거나 API 실패 시 기본 추천 상품(최신 상품 4개) 제공
        if (recommendedProducts.isEmpty()) {
            log.info("API 추천 결과가 없어 기본 추천(최신 상품)을 제공합니다.");
            return productDetailRepository.findProductsByPaging(0, 4);
        }

        log.info("✨ Google Cloud Retail API 맞춤 추천 성공! (조회된 상품 수: {})", recommendedProducts.size());
        return recommendedProducts;
    }
}
