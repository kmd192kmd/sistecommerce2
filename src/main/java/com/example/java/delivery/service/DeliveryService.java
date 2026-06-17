package com.example.java.delivery.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.delivery.entity.Delivery;
import com.example.java.delivery.entity.DeliveryCompany;
import com.example.java.delivery.entity.DeliveryHistory;
import com.example.java.delivery.entity.Hub;
import com.example.java.delivery.repository.DeliveryHistoryRepository;
import com.example.java.delivery.repository.DeliveryRepository;
import com.example.java.delivery.repository.HubRepository;
import com.example.java.common.util.SnowflakeIdGenerator;
import com.example.java.orders.entity.Orders;
import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.repository.OrderItemRepository;
import com.example.java.product.entity.Options;
import com.example.java.product.entity.Product;
import com.example.java.product.entity.Seller;
import com.example.java.product.repository.OptionsRepository;
import com.example.java.product.repository.SellerRepository;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final HubRepository hubRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;
    private final KakaoMapService kakaoMapService;
    private final HolidayService holidayService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final OrderItemRepository orderItemRepository;
    private final OptionsRepository optionsRepository;
    private final SellerRepository sellerRepository;


    /**
     * Finds the intermediate hub that results in the shortest path (HQ -> Mid -> Destination).
     * 본사허브와 배송지 사이에 최단거리가 되는 hub를 찾는 로직
     */
    public Hub findOptimalIntermediateHub(double destLat, double destLon, Hub hqHub, List<Hub> midHubs) {
        Hub optimalHub = null;
        double minDistance = Double.MAX_VALUE;

        for (Hub midHub : midHubs) {
            double hqToMid = kakaoMapService.getDrivingDistanceMeters(hqHub.getLatitude(), hqHub.getLongitude(), midHub.getLatitude(), midHub.getLongitude());
            double midToDest = kakaoMapService.getDrivingDistanceMeters(midHub.getLatitude(), midHub.getLongitude(), destLat, destLon);
            double totalDist = hqToMid + midToDest;

            if (totalDist < minDistance) {
                minDistance = totalDist;
                optimalHub = midHub;
            }
        }
        return optimalHub;
    }

    /**
     * Calculates the estimated delivery date and time based on routing coordinates.
     * Excludes weekends and holidays.
     * 도착 예정일을 계산하는 로직
     */
    public LocalDateTime calculateEstimatedDeliveryDateTime(double destLat, double destLon, LocalDateTime paymentDateTime) {
        // 1. Get Hubs
        List<Hub> allHubs = hubRepository.findAll();
        Hub hqHub = allHubs.stream()
                .filter(h -> "본사허브".equals(h.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("본사허브가 존재하지 않습니다."));

        List<Hub> midHubs = allHubs.stream()
                .filter(h -> !"본사허브".equals(h.getName()))
                .toList();

        // 2. Optimal Hub Routing
        Hub optimalMidHub = findOptimalIntermediateHub(destLat, destLon, hqHub, midHubs);
        double distHQToMid = kakaoMapService.getDrivingDistanceMeters(hqHub.getLatitude(), hqHub.getLongitude(), optimalMidHub.getLatitude(), optimalMidHub.getLongitude());
        double distMidToDest = kakaoMapService.getDrivingDistanceMeters(optimalMidHub.getLatitude(), optimalMidHub.getLongitude(), destLat, destLon);

        // 3. Dispatch Date Calculation (Payment date + 2 business days)
        LocalDate dispatchDate = paymentDateTime.toLocalDate();
        int prepDays = 0;
        while (prepDays < 2) {
            dispatchDate = dispatchDate.plusDays(1);
            if (!holidayService.isNonBusinessDay(dispatchDate)) {
                prepDays++;
            }
        }
        LocalDateTime dispatchAt = dispatchDate.atTime(6, 0); // Departs at 06:00 AM

        // 4. Transit Hours Calculation (HQ -> Mid at 60km/h, Mid -> Dest at 40km/h)
        double hours = (distHQToMid / 60000.0) + (distMidToDest / 40000.0);
        int transitHours = (int) Math.ceil(hours);

        // 5. Add Transit Hours (No skipping non-business days during transit)
        return dispatchAt.plusHours(transitHours);
    }


    /**
     * Calculates the distance surcharge based on the total routing distance.
     */
    public int calculateDistanceSurcharge(double totalDistanceMeters) {
        double distKm = totalDistanceMeters / 1000.0;
        if (distKm > 300) {
            return 5000;
        } else if (distKm > 200) {
            return 3000;
        } else if (distKm > 100) {
            return 1000;
        }
        return 0;
    }

    /**
     * Core method to initialize a Delivery entity when an order is completed/paid.
     * This creates a READY delivery, which will be picked up by the batch process later.
     * 
     * @param orderType 주문 타입 ("B2C": 일반주문, "B2B": 발주, "RETURN": 반품 등)
     */
    @Transactional
    public List<Delivery> createDelivery(Orders order, String recipientName, String recipientPhone, String requestMemo, String orderType) {
        // 1. Find order items by order.seq
        List<OrderItem> items = orderItemRepository.findByOrderSeq(order.getSeq());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 존재하지 않습니다.");
        }

        // 2. Map each item to its delivery company
        Map<DeliveryCompany, List<OrderItem>> companyItemsMap = new HashMap<>();
        for (OrderItem item : items) {
            Options options = optionsRepository.findById(item.getOptionsSeq())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션입니다."));
            Product product = options.getProduct();
            if (product == null) {
                throw new IllegalStateException("옵션에 상품 정보가 연결되어 있지 않습니다.");
            }
            Seller seller = sellerRepository.findById(product.getSellerSeq())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매처입니다."));
            DeliveryCompany company = seller.getDeliveryCompany();
            if (company == null) {
                throw new IllegalStateException("판매처에 연결된 택배사가 없습니다.");
            }

            companyItemsMap.computeIfAbsent(company, k -> new ArrayList<>()).add(item);
        }

        List<Delivery> savedDeliveries = new ArrayList<>();

        // 3. Get optimal route info (common for destination)
        List<Hub> allHubs = hubRepository.findAll();
        Hub hqHub = allHubs.stream()
                .filter(h -> "본사허브".equals(h.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("본사허브가 존재하지 않습니다."));
        List<Hub> midHubs = allHubs.stream()
                .filter(h -> !"본사허브".equals(h.getName()))
                .toList();

        Hub optimalMidHub = findOptimalIntermediateHub(order.getCurrLatitude(), order.getCurrLongitude(), hqHub, midHubs);
        double distHQToMid = kakaoMapService.getDrivingDistanceMeters(hqHub.getLatitude(), hqHub.getLongitude(), optimalMidHub.getLatitude(), optimalMidHub.getLongitude());
        double distMidToDest = kakaoMapService.getDrivingDistanceMeters(optimalMidHub.getLatitude(), optimalMidHub.getLongitude(), order.getCurrLatitude(), order.getCurrLongitude());
        double totalDistance = distHQToMid + distMidToDest;

        // int distanceSurcharge = calculateDistanceSurcharge(totalDistance);
        int distanceSurcharge = 0;
        LocalDateTime dispatchAt = paymentDateToDispatchAt(LocalDateTime.now());
        double hours = (distHQToMid / 60000.0) + (distMidToDest / 40000.0);
        LocalDateTime estimatedDate = dispatchAt.plusHours((int) Math.ceil(hours));

        // 4. Create a delivery for each distinct DeliveryCompany
        for (DeliveryCompany company : companyItemsMap.keySet()) {
            int totalFee = company.getBase_delivery_fee() + distanceSurcharge;

            String trackingPrefix = "ORD";
            if ("B2B".equalsIgnoreCase(orderType)) trackingPrefix = "B2B";
            else if ("B2C".equalsIgnoreCase(orderType)) trackingPrefix = "B2C";
            else if ("RETURN".equalsIgnoreCase(orderType)) trackingPrefix = "RET";
            String trackingNumber = trackingPrefix + "-" + snowflakeIdGenerator.nextId();

            Delivery delivery = Delivery.builder()
                    .tracking_number(trackingNumber)
                    .recipient_name(recipientName)
                    .recipient_phone(recipientPhone)
                    .status("READY")
                    .request_memo(requestMemo)
                    .dispatch_at(dispatchAt)
                    .estimated_date(estimatedDate)
                    .distance_surcharge(distanceSurcharge)
                    .total_delivery_fee(totalFee)
                    .deliveryCompany(company)
                    .orders(order)
                    .delayHours(0)
                    .build();

            Delivery savedDelivery = deliveryRepository.save(delivery);

            deliveryHistoryRepository.save(DeliveryHistory.builder()
                    .location("SENDER")
                    .currLatitude(order.getCurrLatitude())
                    .currLongitude(order.getCurrLongitude())
                    .arrivedAt(LocalDateTime.now())
                    .delivery(savedDelivery)
                    .build());

            savedDeliveries.add(savedDelivery);
        }

        return savedDeliveries;
    }

    private LocalDateTime paymentDateToDispatchAt(LocalDateTime paymentTime) {
        LocalDate dispatchDate = paymentTime.toLocalDate();
        int prepDays = 0;
        while (prepDays < 2) {
            dispatchDate = dispatchDate.plusDays(1);
            if (!holidayService.isNonBusinessDay(dispatchDate)) {
                prepDays++;
            }
        }
        return dispatchDate.atTime(6, 0);
    }

    /**
     * 특정 주문의 모든 배송을 취소 상태(CANCELED)로 변경한다.
     */
    @Transactional
    public void cancelAllDeliveriesForOrder(Long orderSeq) {
        List<Delivery> deliveries = deliveryRepository.findByOrders_Seq(orderSeq);
        if (deliveries == null || deliveries.isEmpty()) {
            return;
        }
        for (Delivery d : deliveries) {
            d.setStatus("CANCELED");
        }
        deliveryRepository.saveAll(deliveries);
    }

    /**
     * 특정 주문 내에서 지정된 택배사(들)에 속한 배송 레코드만 취소 상태(CANCELED)로 변경한다.
     */
    @Transactional
    public void cancelDeliveriesForOrderAndCompanies(Long orderSeq, List<String> companyNames) {
        if (companyNames == null || companyNames.isEmpty()) {
            return;
        }
        List<Delivery> deliveries = deliveryRepository.findByOrders_Seq(orderSeq);
        if (deliveries == null || deliveries.isEmpty()) {
            return;
        }
        for (Delivery d : deliveries) {
            String companyName = (d.getDeliveryCompany() != null) ? d.getDeliveryCompany().getName() : "배송 준비중";
            if (companyNames.contains(companyName)) {
                d.setStatus("CANCELED");
            }
        }
        deliveryRepository.saveAll(deliveries);
    }
}
