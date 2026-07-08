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
import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.repository.OrderItemRepository;
import com.example.java.orders.repository.OrdersRepository;
import com.example.java.product.repository.OptionsRepository;
import com.example.java.product.repository.SellerRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Slf4j
@Configuration
public class DeliveryBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DeliveryRepository deliveryRepository;
    private final HubRepository hubRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;
    private final DeliveryService deliveryService;
    private final KakaoMapService kakaoMapService;
    private final OrderItemRepository orderItemRepository;
    private final OrdersRepository ordersRepository;
    private final OptionsRepository optionsRepository;
    private final SellerRepository sellerRepository;
    private final Random random = new Random();

    public DeliveryBatchConfig(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               DeliveryRepository deliveryRepository,
                               HubRepository hubRepository,
                               DeliveryHistoryRepository deliveryHistoryRepository,
                               DeliveryService deliveryService,
                               KakaoMapService kakaoMapService,
                               OrderItemRepository orderItemRepository,
                               OrdersRepository ordersRepository,
                               OptionsRepository optionsRepository,
                               SellerRepository sellerRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.deliveryRepository = deliveryRepository;
        this.hubRepository = hubRepository;
        this.deliveryHistoryRepository = deliveryHistoryRepository;
        this.deliveryService = deliveryService;
        this.kakaoMapService = kakaoMapService;
        this.orderItemRepository = orderItemRepository;
        this.ordersRepository = ordersRepository;
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
                Orders order = delivery.getOrders();
                if (order != null) {
                    try {
                        // 1. 일반 조회 (엔티티의 @Version을 통해 낙관적 락 자동 발동)
                        Orders lockedOrder = ordersRepository.findById(order.getSeq())
                                .orElse(order);

                        if (lockedOrder.getOrderStatus() != null && lockedOrder.getOrderStatus() == 9) {
                            delivery.setStatus("CANCELED");
                            deliveryRepository.save(delivery);
                            continue;
                        }

                        delivery.setStatus("SHIPPING");
                        delivery.setDelayHours(0);
                        deliveryRepository.save(delivery);

                        lockedOrder.setOrderStatus(5);
                        ordersRepository.save(lockedOrder); // 👈 여기서 버전 체크 후 다르면 예외 터짐

                        List<OrderItem> items = orderItemRepository.findByOrderSeq(lockedOrder.getSeq());
                        for (OrderItem item : items) {
                            if (item.getItemStatus() != null && item.getItemStatus() == 6) {
                                continue;
                            }
                            item.setItemStatus(2); 
                        }
                        orderItemRepository.saveAll(items);

                    } catch (ObjectOptimisticLockingFailureException e) {
                        // 2. 다른 트랜잭션(예: 유저의 주문취소)이 먼저 데이터를 수정해서 버전이 안 맞을 때 예외 처리
                        // 배치(Tasklet) 특성상 해당 건은 스킵하고 다음 루프로 넘어가거나 재시도 로직을 태워야 합니다.
                        log.warn("낙관적 락 충돌 발생 - 주문번호: {}, 다음 배치에서 재처리합니다.", order.getSeq());
                        continue; 
                    }
                } else {
                    delivery.setStatus("SHIPPING");
                    delivery.setDelayHours(0);
                    deliveryRepository.save(delivery);
                }

                // 배송 이력 저장
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

                // 낙관적 락으로 조회
                Orders lockedOrder = ordersRepository.findBySeq(order.getSeq())
                        .orElse(order);

                // 대기 중 이미 취소된 주문(9)인 경우 더 배송 진행을 하지 않고 CANCELED 처리
                if (lockedOrder.getOrderStatus() != null && lockedOrder.getOrderStatus() == 9) {
                    delivery.setStatus("CANCELED");
                    deliveryRepository.save(delivery);
                    continue;
                }
                order = lockedOrder;

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
                    List<OrderItem> orderItems = orderItemRepository.findByOrderSeq(order.getSeq());
                    for (OrderItem item : orderItems) {
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

                    // 주문 내 모든 상품이 배송완료(3) 또는 취소/반품(6, 9) 상태이면 주문 전체 상태도 배송완료(6)로 업데이트
                    boolean allDelivered = orderItems.stream()
                            .allMatch(item -> item.getItemStatus() != null && (item.getItemStatus() == 3 || item.getItemStatus() == 6 || item.getItemStatus() == 9));
                    if (allDelivered) {
                        order.setOrderStatus(6);
                        ordersRepository.save(order);
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
