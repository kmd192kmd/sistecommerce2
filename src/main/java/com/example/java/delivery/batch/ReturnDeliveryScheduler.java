package com.example.java.delivery.batch;

import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.ReturnRequest;
import com.example.java.orders.entity.Returns;
import com.example.java.orders.repository.OrderItemRepository;
import com.example.java.orders.repository.ReturnRequestRepository;
import com.example.java.orders.repository.ReturnsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnDeliveryScheduler {

    private final ReturnsRepository returnsRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 매일 저녁 6시에 반품 배송 상태를 체크하여, 반품 신청 후 48시간이 지난 건들을 자동으로 완료 처리한다.
     */
    @Scheduled(cron = "0 0 18 * * *") // 매일 저녁 6시(18시) 정각에 실행
    @Transactional
    public void autoCompleteReturnDeliveries() {
        LocalDateTime thresholdComplete = LocalDateTime.now().minusHours(48);
        LocalDateTime thresholdProgress = LocalDateTime.now().minusHours(24);
        
        // RETURNING 상태인 모든 반품 배송 조회
        List<Returns> shippingReturns = returnsRepository.findByStatus("RETURNING");
        if (shippingReturns.isEmpty()) {
            return;
        }

        log.info("[ReturnScheduler] Checking {} in-progress return deliveries...", shippingReturns.size());

        for (Returns ret : shippingReturns) {
            ReturnRequest req = ret.getReturnRequest();
            if (req == null) continue;

            // 1. 반품 신청 시간(requestDate) 기준 48시간 경과 건 -> 반품 완료(9) 처리
            if (req.getRequestDate() != null && req.getRequestDate().isBefore(thresholdComplete)) {
                LocalDateTime now = LocalDateTime.now();

                // returns 테이블 상태 변경 및 완료일 설정
                ret.setStatus("RETURNED");
                ret.setCompletedDate(now);
                returnsRepository.save(ret);

                // return_request 테이블 상태(9: 완료) 및 완료일 설정
                req.setStatus(9);
                req.setCompletedDate(now);
                req.setDecisionDate(now);
                returnRequestRepository.save(req);

                // order_item 테이블 상태(9: 반품완료)로 변경
                OrderItem item = orderItemRepository.findById(req.getOrderItemSeq()).orElse(null);
                if (item != null) {
                    item.setItemStatus(9);
                    orderItemRepository.save(item);
                }

                log.info("[ReturnScheduler] Auto-completed return for returnRequestSeq={}, trackingNumber={}", req.getSeq(), ret.getTrackingNumber());
            } 
            // 2. 반품 신청 시간(requestDate) 기준 24시간 경과 건 -> 반품 진행중(8) 처리
            else if (req.getRequestDate() != null && req.getRequestDate().isBefore(thresholdProgress)) {
                OrderItem item = orderItemRepository.findById(req.getOrderItemSeq()).orElse(null);
                if (item != null && item.getItemStatus() == 7) {
                    item.setItemStatus(8);
                    orderItemRepository.save(item);
                    log.info("[ReturnScheduler] Auto-progressed return to IN_PROGRESS (8) for returnRequestSeq={}", req.getSeq());
                }
            }
        }
    }
}
