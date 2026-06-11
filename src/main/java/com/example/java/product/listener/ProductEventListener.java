package com.example.java.product.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.java.product.document.ProductDocument;
import com.example.java.product.event.ProductUpdatedEvent;
import com.example.java.product.repository.ProductDetailRepository;
import com.example.java.product.repository.ProductSearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener {

    private final ProductDetailRepository productDetailRepository;
    private final ProductSearchRepository productSearchRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductUpdated(ProductUpdatedEvent event) {
        log.info("Elasticsearch 동기화 시작 - Product Seq: {}", event.getProductSeq());
        try {
            productDetailRepository.findById(event.getProductSeq()).ifPresentOrElse(product -> {
                ProductDocument doc = ProductDocument.builder()
                        .id(product.getSeq())
                        .sellerSeq(product.getSellerSeq())
                        .categorySeq(product.getCategorySeq())
                        .productName(product.getProductName())
                        .price(product.getPrice())
                        .saleStatus(product.getSaleStatus())
                        .approvalStatus(product.getApprovalStatus())
                        .hideYn(product.getHideYn())
                        .viewCount(product.getViewCount())
                        .avgRating(product.getAvgRating())
                        .reviewCount(product.getReviewCount())
                        .salesCount(product.getSalesCount())
                        .createdDate(product.getCreatedDate())
                        .status(product.getStatus())
                        .thumbnailUrl(product.getThumbnailUrl())
                        .build();

                productSearchRepository.save(doc);
                log.info("Elasticsearch 동기화 완료 - Product Seq: {}", event.getProductSeq());
            }, () -> {
                productSearchRepository.deleteById(event.getProductSeq());
                log.info("Elasticsearch 데이터 삭제 완료 (DB 미존재) - Product Seq: {}", event.getProductSeq());
            });
        } catch (Exception e) {
            log.error("Elasticsearch 동기화 실패 - Product Seq: {}", event.getProductSeq(), e);
        }
    }
}
