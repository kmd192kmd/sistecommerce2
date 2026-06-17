package com.example.java.retail.service;

import com.google.cloud.retail.v2.ProductDetail;
import com.google.cloud.retail.v2.UserEvent;
import com.google.cloud.retail.v2.UserEventServiceClient;
import com.google.cloud.retail.v2.WriteUserEventRequest;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetailUserEventService {

    private final UserEventServiceClient userEventServiceClient;

    @Value("${google.cloud.project-id}")
    private String projectId;

    @Value("${retail.location:global}")
    private String location;

    @Value("${retail.catalog:default_catalog}")
    private String catalog;

    /**
     * 사용자의 행동 로그를 구글 Retail AI로 실시간 전송합니다.
     */
    @Async
    public void sendUserEvent(Long memberSeq, Long productSeq, String eventType) {
        try {
            String parent = String.format("projects/%s/locations/%s/catalogs/%s", projectId, location, catalog);

            UserEvent.Builder userEventBuilder = UserEvent.newBuilder()
                    .setEventType(eventType)
                    .setVisitorId(memberSeq != null ? String.valueOf(memberSeq) : UUID.randomUUID().toString())
                    .setEventTime(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build())
                    .addProductDetails(ProductDetail.newBuilder()
                            .setProduct(com.google.cloud.retail.v2.Product.newBuilder()
                                    .setId(String.valueOf(productSeq))
                                    .build())
                            .setQuantity(com.google.protobuf.Int32Value.newBuilder().setValue(1).build())
                            .build());

            if ("purchase-complete".equals(eventType)) {
                userEventBuilder.setPurchaseTransaction(com.google.cloud.retail.v2.PurchaseTransaction.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setRevenue(100.0f)
                        .setCurrencyCode("KRW")
                        .build());
            }

            UserEvent userEvent = userEventBuilder.build();

            WriteUserEventRequest writeRequest = WriteUserEventRequest.newBuilder()
                    .setParent(parent)
                    .setUserEvent(userEvent)
                    .build();

            userEventServiceClient.writeUserEvent(writeRequest);
            log.info("Retail AI 실시간 사용자 행동 이벤트 전송 완료 (이벤트: {}, 상품: {}, 회원: {})", eventType, productSeq, memberSeq);

        } catch (Exception e) {
            log.error("Retail AI 실시간 사용자 행동 이벤트 전송 실패", e);
        }
    }
}
