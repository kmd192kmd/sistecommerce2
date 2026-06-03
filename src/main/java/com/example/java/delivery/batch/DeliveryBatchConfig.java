package com.example.java.delivery.batch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.java.delivery.entity.Delivery;
import com.example.java.delivery.entity.DeliveryHistory;
import com.example.java.delivery.entity.Hub;
import com.example.java.delivery.repository.DeliveryHistoryRepository;
import com.example.java.delivery.repository.DeliveryRepository;
import com.example.java.delivery.repository.HubRepository;
import com.example.java.delivery.service.DeliveryService;
import com.example.java.orders.controller.entity.Orders;

@Configuration
public class DeliveryBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DeliveryRepository deliveryRepository;
    private final HubRepository hubRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;
    private final DeliveryService deliveryService;
    private final Random random = new Random();

    public DeliveryBatchConfig(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               DeliveryRepository deliveryRepository,
                               HubRepository hubRepository,
                               DeliveryHistoryRepository deliveryHistoryRepository,
                               DeliveryService deliveryService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.deliveryRepository = deliveryRepository;
        this.hubRepository = hubRepository;
        this.deliveryHistoryRepository = deliveryHistoryRepository;
        this.deliveryService = deliveryService;
    }

    /**
     * Define the Batch Job for Delivery Updates.
     */
    @Bean
    public Job deliveryUpdateJob() {
        return new JobBuilder("deliveryUpdateJob", jobRepository)
                .start(startShippingStep())
                .next(advanceShippingStep())
                .build();
    }

    /**
     * Step 1: Transition deliveries from READY to SHIPPING if dispatch_at <= now.
     */
    @Bean
    public Step startShippingStep() {
        return new StepBuilder("startShippingStep", jobRepository)
                .tasklet(startShippingTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet startShippingTasklet() {
        return (contribution, chunkContext) -> {
            LocalDateTime now = LocalDateTime.now();
            List<Delivery> readyDeliveries = deliveryRepository.findAll().stream()
                    .filter(d -> "READY".equals(d.getStatus()) && d.getDispatch_at() != null && !d.getDispatch_at().isAfter(now))
                    .toList();

            // Headquarter Hub
            Hub hqHub = hubRepository.findAll().stream()
                    .filter(h -> "본사허브".equals(h.getName()))
                    .findFirst()
                    .orElse(null);

            for (Delivery delivery : readyDeliveries) {
                // Determine Leg 1 delay (5% probability)
                boolean isLeg1Delayed = random.nextInt(100) < 5;
                if (isLeg1Delayed) {
                    delivery.setStatus("DELAYED");
                    delivery.setDelayHours(12);
                } else {
                    delivery.setStatus("SHIPPING");
                    delivery.setDelayHours(0);
                }
                deliveryRepository.save(delivery);

                // Add delivery history: Leaving HQ Hub
                DeliveryHistory hqHistory = DeliveryHistory.builder()
                        .location("HUB")
                        .currLatitude(hqHub != null ? hqHub.getLatitude() : 37.5049)
                        .currLongitude(hqHub != null ? hqHub.getLongitude() : 127.0505)
                        .arrivedAt(now)
                        .delivery(delivery)
                        .hub(hqHub)
                        .build();
                deliveryHistoryRepository.save(hqHistory);
            }
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 2: Handle progress of SHIPPING/DELAYED deliveries (simulation of transit and delays).
     */
    @Bean
    public Step advanceShippingStep() {
        return new StepBuilder("advanceShippingStep", jobRepository)
                .tasklet(advanceShippingTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet advanceShippingTasklet() {
        return (contribution, chunkContext) -> {
            LocalDateTime now = LocalDateTime.now();
            List<Delivery> activeDeliveries = deliveryRepository.findAll().stream()
                    .filter(d -> "SHIPPING".equals(d.getStatus()) || "DELAYED".equals(d.getStatus()))
                    .toList();

            for (Delivery delivery : activeDeliveries) {
                // 1. Completion check (Original estimated_date + delayHours)
                LocalDateTime currentEstimatedArrival = delivery.getEstimated_date() != null ?
                        delivery.getEstimated_date().plusHours(delivery.getDelayHours()) : null;

                if (currentEstimatedArrival != null && !currentEstimatedArrival.isAfter(now)) {
                    delivery.setStatus("DELIVERED");
                    delivery.setCompleted_at(now);
                    deliveryRepository.save(delivery);

                    // Add final history: arrived at RECEIVER
                    DeliveryHistory receiverHistory = DeliveryHistory.builder()
                            .location("RECEIVER")
                            .currLatitude(delivery.getOrders() != null ? delivery.getOrders().getCurrLatitude() : 37.5049)
                            .currLongitude(delivery.getOrders() != null ? delivery.getOrders().getCurrLongitude() : 127.0505)
                            .arrivedAt(now)
                            .delivery(delivery)
                            .build();
                    deliveryHistoryRepository.save(receiverHistory);
                    continue;
                }

                // 2. Intermediate Hub arrival check & Leg 2 delay check
                // If 3 hours have passed since dispatch and it hasn't logged the intermediate hub yet
                List<DeliveryHistory> history = deliveryHistoryRepository.findByDeliverySeqOrderBySeqAsc(delivery.getSeq());
                boolean hasIntermediateHubLogged = history.stream()
                        .anyMatch(h -> h.getHub() != null && !"본사허브".equals(h.getHub().getName()));

                if (!hasIntermediateHubLogged && delivery.getDispatch_at() != null && delivery.getDispatch_at().plusHours(3).isBefore(now)) {
                    // Find optimal intermediate hub for this delivery
                    List<Hub> midHubs = hubRepository.findAll().stream()
                            .filter(h -> !"본사허브".equals(h.getName()))
                            .toList();
                    Orders order = delivery.getOrders();
                    if (order != null && !midHubs.isEmpty()) {
                        Hub hqHub = hubRepository.findAll().stream()
                                .filter(h -> "본사허브".equals(h.getName()))
                                .findFirst()
                                .orElse(null);
                        
                        Hub optimalHub = deliveryService.findOptimalIntermediateHub(
                                order.getCurrLatitude(), order.getCurrLongitude(), hqHub, midHubs);

                        if (optimalHub != null) {
                            DeliveryHistory midHistory = DeliveryHistory.builder()
                                    .location("HUB")
                                    .currLatitude(optimalHub.getLatitude())
                                    .currLongitude(optimalHub.getLongitude())
                                    .arrivedAt(now)
                                    .delivery(delivery)
                                    .hub(optimalHub)
                                    .build();
                            deliveryHistoryRepository.save(midHistory);

                            // Determine Leg 2 delay (5% probability)
                            boolean isLeg2Delayed = random.nextInt(100) < 5;
                            if (isLeg2Delayed) {
                                delivery.setStatus("DELAYED");
                                delivery.setDelayHours(delivery.getDelayHours() + 12);
                            } else {
                                // If Leg 1 was delayed, it stays DELAYED, otherwise SHIPPING
                                if (!"DELAYED".equals(delivery.getStatus())) {
                                    delivery.setStatus("SHIPPING");
                                }
                            }
                            deliveryRepository.save(delivery);
                        }
                    }
                }
            }
            return RepeatStatus.FINISHED;
        };
    }
}

/* =========================================================================
   [ 포트폴리오 비교용: Spring @Scheduled 버전 스케줄러 구현 코드 (주석 처리) ]
   -------------------------------------------------------------------------
   Spring Batch는 대용량 처리에 특화된 Chunk 지향 구조, 로깅, 재시도/건너뛰기,
   트랜잭션 세분화 및 실패 시 복구(JobRepository 기반) 등의 큰 이점이 있습니다.
   반면 @Scheduled는 구조가 간단하지만, 예외 발생 시 직접 복구 처리를 다루어야 하며,
   단일 스레드 기반이 될 경우 다른 스케줄러를 블로킹할 수 있어 대용량 배치 처리에는
   부적합합니다.

package com.example.java.delivery.batch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.example.java.delivery.entity.Delivery;
import com.example.java.delivery.entity.DeliveryHistory;
import com.example.java.delivery.entity.Hub;
import com.example.java.delivery.repository.DeliveryHistoryRepository;
import com.example.java.delivery.repository.DeliveryRepository;
import com.example.java.delivery.repository.HubRepository;
import com.example.java.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeliveryScheduler {

    private final DeliveryRepository deliveryRepository;
    private final HubRepository hubRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;
    private final DeliveryService deliveryService;
    private final Random random = new Random();

    // 1시간마다 주기적으로 배송 출발 처리 및 배송 상태 업데이트
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void runDeliveryUpdateScheduler() {
        LocalDateTime now = LocalDateTime.now();

        // 1. READY -> SHIPPING 출발 처리
        List<Delivery> readyDeliveries = deliveryRepository.findAll().stream()
                .filter(d -> "READY".equals(d.getStatus()) && d.getDispatch_at() != null && !d.getDispatch_at().isAfter(now))
                .toList();

        Hub hqHub = hubRepository.findAll().stream()
                .filter(h -> "본사허브".equals(h.getName()))
                .findFirst()
                .orElse(null);

        for (Delivery delivery : readyDeliveries) {
            delivery.setStatus("SHIPPING");
            deliveryRepository.save(delivery);

            DeliveryHistory hqHistory = DeliveryHistory.builder()
                    .location("HUB")
                    .currLatitude(hqHub != null ? hqHub.getLatitude() : 37.5049)
                    .currLongitude(hqHub != null ? hqHub.getLongitude() : 127.0505)
                    .arrivedAt(now)
                    .delivery(delivery)
                    .hub(hqHub)
                    .build();
            deliveryHistoryRepository.save(hqHistory);
        }

        // 2. SHIPPING/DELAYED 배송 진행 시뮬레이션
        List<Delivery> activeDeliveries = deliveryRepository.findAll().stream()
                .filter(d -> "SHIPPING".equals(d.getStatus()) || "DELAYED".equals(d.getStatus()))
                .toList();

        for (Delivery delivery : activeDeliveries) {
            // 완료 판정
            if (delivery.getEstimated_date() != null && !delivery.getEstimated_date().isAfter(now)) {
                delivery.setStatus("DELIVERED");
                delivery.setCompleted_at(now);
                deliveryRepository.save(delivery);

                DeliveryHistory receiverHistory = DeliveryHistory.builder()
                        .location("RECEIVER")
                        .currLatitude(delivery.getOrders() != null ? delivery.getOrders().getCurrLatitude() : 37.5049)
                        .currLongitude(delivery.getOrders() != null ? delivery.getOrders().getCurrLongitude() : 127.0505)
                        .arrivedAt(now)
                        .delivery(delivery)
                        .build();
                deliveryHistoryRepository.save(receiverHistory);
                continue;
            }

            // 5% 확률 지연 발생 시뮬레이션
            if ("SHIPPING".equals(delivery.getStatus()) && random.nextInt(100) < 5) {
                delivery.setStatus("DELAYED");
                delivery.setEstimated_date(delivery.getEstimated_date().plusHours(12));
                deliveryRepository.save(delivery);
                continue;
            }

            // 중간 허브 경유지 처리
            List<DeliveryHistory> history = deliveryHistoryRepository.findByDeliverySeqOrderBySeqAsc(delivery.getSeq());
            boolean hasIntermediateHubLogged = history.stream()
                    .anyMatch(h -> h.getHub() != null && !"본사허브".equals(h.getHub().getName()));

            if (!hasIntermediateHubLogged && delivery.getDispatch_at() != null && delivery.getDispatch_at().plusHours(3).isBefore(now)) {
                List<Hub> midHubs = hubRepository.findAll().stream()
                        .filter(h -> !"본사허브".equals(h.getName()))
                        .toList();
                if (delivery.getOrders() != null && !midHubs.isEmpty()) {
                    Hub optimalHub = deliveryService.findOptimalIntermediateHub(
                            delivery.getOrders().getCurrLatitude(), delivery.getOrders().getCurrLongitude(), hqHub, midHubs);
                    if (optimalHub != null) {
                        DeliveryHistory midHistory = DeliveryHistory.builder()
                                .location("HUB")
                                .currLatitude(optimalHub.getLatitude())
                                .currLongitude(optimalHub.getLongitude())
                                .arrivedAt(now)
                                .delivery(delivery)
                                .hub(optimalHub)
                                .build();
                        deliveryHistoryRepository.save(midHistory);
                    }
                }
            }
        }
    }
}
========================================================================= */
