package com.example.java.delivery.batch;

import com.example.java.delivery.entity.Delivery;
import com.example.java.delivery.entity.DeliveryHistory;
import com.example.java.delivery.entity.Hub;
import com.example.java.delivery.repository.DeliveryHistoryRepository;
import com.example.java.delivery.repository.DeliveryRepository;
import com.example.java.delivery.repository.HubRepository;
import com.example.java.delivery.service.DeliveryService;
import com.example.java.delivery.service.KakaoMapService;
import com.example.java.orders.entity.Orders;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;


@Configuration
public class DeliveryBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DeliveryRepository deliveryRepository;
    private final HubRepository hubRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;
    private final DeliveryService deliveryService;
    private final KakaoMapService kakaoMapService;
    private final com.example.java.orders.repository.OrderItemRepository orderItemRepository;
    private final com.example.java.product.repository.OptionsRepository optionsRepository;
    private final com.example.java.product.repository.SellerRepository sellerRepository;
    private final Random random = new Random();

    public DeliveryBatchConfig(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               DeliveryRepository deliveryRepository,
                               HubRepository hubRepository,
                               DeliveryHistoryRepository deliveryHistoryRepository,
                               DeliveryService deliveryService,
                               KakaoMapService kakaoMapService,
                               com.example.java.orders.repository.OrderItemRepository orderItemRepository,
                               com.example.java.product.repository.OptionsRepository optionsRepository,
                               com.example.java.product.repository.SellerRepository sellerRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.deliveryRepository = deliveryRepository;
        this.hubRepository = hubRepository;
        this.deliveryHistoryRepository = deliveryHistoryRepository;
        this.deliveryService = deliveryService;
        this.kakaoMapService = kakaoMapService;
        this.orderItemRepository = orderItemRepository;
        this.optionsRepository = optionsRepository;
        this.sellerRepository = sellerRepository;
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

            Hub hqHub = hubRepository.findAll().stream()
                    .filter(h -> "본사허브".equals(h.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("시스템 치명적 오류: DB에 '본사허브' 데이터가 없습니다!"));

            for (Delivery delivery : readyDeliveries) {
                // 본사 출발 (지연 없음)
                delivery.setStatus("SHIPPING");
                delivery.setDelayHours(0);
                deliveryRepository.save(delivery);

                // Add delivery history: 본사 허브 도착 기록
                DeliveryHistory hqHistory = DeliveryHistory.builder()
                        .location("HUB")
                        .currLatitude(hqHub.getLatitude())
                        .currLongitude(hqHub.getLongitude())
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

            List<Hub> allHubs = hubRepository.findAll();
            Hub hqHub = allHubs.stream().filter(h -> "본사허브".equals(h.getName())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("시스템 치명적 오류: DB에 '본사허브' 데이터가 없습니다!"));
            List<Hub> midHubs = allHubs.stream().filter(h -> !"본사허브".equals(h.getName())).toList();

            for (Delivery delivery : activeDeliveries) {
                Orders order = delivery.getOrders();
                if (order == null || midHubs.isEmpty() || delivery.getDispatch_at() == null) continue;

                // 1. 중간 허브 찾기 및 예상 도착 시간 계산
                Hub optimalHub = deliveryService.findOptimalIntermediateHub(
                        order.getCurrLatitude(), order.getCurrLongitude(), hqHub, midHubs);
                if (optimalHub == null) continue;

                double distHQToMid = kakaoMapService.getDrivingDistanceMeters(hqHub.getLatitude(), hqHub.getLongitude(), optimalHub.getLatitude(), optimalHub.getLongitude());
                double hqToMidHours = distHQToMid / 60000.0;
                // 본사 출발시간 기준 + 실제 거리 계산 소요시간
                LocalDateTime midHubArrivedAt = delivery.getDispatch_at().plusMinutes((long) (hqToMidHours * 60));

                List<DeliveryHistory> history = deliveryHistoryRepository.findByDeliverySeqOrderBySeqAsc(delivery.getSeq());
                boolean hasIntermediateHubLogged = history.stream()
                        .anyMatch(h -> h.getHub() != null && !"본사허브".equals(h.getHub().getName()));

                // 2. 중간 허브 도착 처리 및 최종 목적지 지연/실패 확률 적용
                if (!hasIntermediateHubLogged && !now.isBefore(midHubArrivedAt)) {
                    DeliveryHistory midHistory = DeliveryHistory.builder()
                            .location("HUB")
                            .currLatitude(optimalHub.getLatitude())
                            .currLongitude(optimalHub.getLongitude())
                            .arrivedAt(now) // 배치가 실행된 시점을 기준 도착으로 기록
                            .delivery(delivery)
                            .hub(optimalHub)
                            .build();
                    deliveryHistoryRepository.save(midHistory);

                    // 확률 기반 로직 적용 (최종 배송 구간: 정상 80%, 실패 5%, 이틀 지연 5%, 하루 지연 10%)
                    int randomValue = random.nextInt(100);
                    if (randomValue < 5) {
                        delivery.setStatus("FAILED");
                    } else if (randomValue < 10) {
                        delivery.setStatus("DELAYED");
                        delivery.setDelayHours(48); // 이틀 지연
                    } else if (randomValue < 20) {
                        delivery.setStatus("DELAYED");
                        delivery.setDelayHours(24); // 하루 지연
                    } else {
                        delivery.setStatus("SHIPPING"); // 정상 배송
                    }
                    deliveryRepository.save(delivery);
                    continue; // 상태 변경 후 다음 배송건으로
                }

                // 3. 최종 목적지 도착 처리
                if ("FAILED".equals(delivery.getStatus())) continue;

                // 지연 시간이 포함된 최종 도착 예정일 계산
                LocalDateTime currentEstimatedArrival = delivery.getEstimated_date() != null ?
                        delivery.getEstimated_date().plusHours(delivery.getDelayHours()) : null;

                // 이미 중간 허브를 거쳤고, 최종 도착 시간을 넘겼다면 배송 완료 처리
                if (hasIntermediateHubLogged && currentEstimatedArrival != null && !now.isBefore(currentEstimatedArrival)) {
                    delivery.setStatus("DELIVERED");
                    delivery.setCompleted_at(now);
                    deliveryRepository.save(delivery);

                    // 배송 완료 시 해당 배송에 포함된 주문상품들의 상태도 배송완료(3)로 변경
                    List<com.example.java.orders.entity.OrderItem> orderItems = orderItemRepository.findByOrderSeq(order.getSeq());
                    for (com.example.java.orders.entity.OrderItem item : orderItems) {
                        optionsRepository.findById(item.getOptionsSeq()).ifPresent(opt -> {
                            if (opt.getProduct() != null && opt.getProduct().getSellerSeq() != null) {
                                sellerRepository.findById(opt.getProduct().getSellerSeq()).ifPresent(sel -> {
                                    if (sel.getDeliveryCompany() != null && delivery.getDeliveryCompany() != null && 
                                        sel.getDeliveryCompany().getSeq().equals(delivery.getDeliveryCompany().getSeq())) {
                                        item.setItemStatus(3);
                                        orderItemRepository.save(item);
                                    }
                                });
                            }
                        });
                    }

                    DeliveryHistory receiverHistory = DeliveryHistory.builder()
                            .location("DESTINATION")
                            .currLatitude(order.getCurrLatitude())
                            .currLongitude(order.getCurrLongitude())
                            .arrivedAt(now)
                            .delivery(delivery)
                            .build();
                    deliveryHistoryRepository.save(receiverHistory);
                }
            }
            return RepeatStatus.FINISHED;
        };
    }
}
