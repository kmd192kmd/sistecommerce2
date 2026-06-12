package com.example.java.orders.scheduler;

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
     * 매일 새벽 6시에 반품 배송 상태를 체크하여, 반품 신청 후 48시간이 지난 건들을 자동으로 완료 처리한다.
     */
    @Scheduled(cron = "0 0 6 * * *") // 매일 새벽 6시 정각에 실행
    @Transactional
    public void autoCompleteReturnDeliveries() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);
        
        // SHIPPING 상태인 모든 반품 배송 조회
        List<Returns> shippingReturns = returnsRepository.findByStatus("SHIPPING");
        if (shippingReturns.isEmpty()) {
            return;
        }

        log.info("[ReturnScheduler] Checking {} in-progress return deliveries...", shippingReturns.size());

        for (Returns ret : shippingReturns) {
            ReturnRequest req = ret.getReturnRequest();
            if (req == null) continue;

            // 반품 신청 시간(requestDate) 기준 48시간 경과 확인
            if (req.getRequestDate() != null && req.getRequestDate().isBefore(threshold)) {
                LocalDateTime now = LocalDateTime.now();

                // 1. returns 테이블 상태 변경 및 완료일 설정
                ret.setStatus("DELIVERED");
                ret.setCompletedDate(now);
                returnsRepository.save(ret);

                // 2. return_request 테이블 상태(9: 완료) 및 완료일 설정
                req.setStatus(9);
                req.setCompletedDate(now);
                req.setDecisionDate(now);
                returnRequestRepository.save(req);

                // 3. order_item 테이블 상태(9: 반품완료)로 변경
                OrderItem item = orderItemRepository.findById(req.getOrderItemSeq()).orElse(null);
                if (item != null) {
                    item.setItemStatus(9);
                    orderItemRepository.save(item);
                }

                log.info("[ReturnScheduler] Auto-completed return for returnRequestSeq={}, trackingNumber={}", req.getSeq(), ret.getTrackingNumber());
            }
        }
    }
}
