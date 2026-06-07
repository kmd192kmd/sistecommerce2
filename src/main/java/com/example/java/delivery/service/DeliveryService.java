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
import com.example.java.orders.controller.entity.Orders;

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
    private final Random random = new Random();


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
    public Delivery createDelivery(Orders order, DeliveryCompany company, String recipientName, String recipientPhone, String requestMemo, String orderType) {
        // 1. Get optimal route
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

        // 2. Surcharge & Total Fee
        int distanceSurcharge = calculateDistanceSurcharge(totalDistance);
        int totalFee = company.getBase_delivery_fee() + distanceSurcharge;

        // 3. Dispatch Date & Timeline Calculation
        LocalDateTime dispatchAt = paymentDateToDispatchAt(LocalDateTime.now());
        double hours = (distHQToMid / 60000.0) + (distMidToDest / 40000.0);
        LocalDateTime estimatedDate = dispatchAt.plusHours((int) Math.ceil(hours));

        // 4. Generate unique tracking number based on orderType
        String trackingPrefix = "ORD";
        if ("B2B".equalsIgnoreCase(orderType)) trackingPrefix = "B2B";
        else if ("B2C".equalsIgnoreCase(orderType)) trackingPrefix = "B2C";
        else if ("RETURN".equalsIgnoreCase(orderType)) trackingPrefix = "RET";
        String trackingNumber = trackingPrefix + "-" + System.currentTimeMillis() + "-" + (1000 + random.nextInt(9000));

        // 5. Save Delivery
        Delivery delivery = Delivery.builder()
                .tracking_number(trackingNumber)
                .recipient_name(recipientName)
                .recipient_phone(recipientPhone)
                .status("READY") // 정상적으로 대기 상태로 초기화 (배치가 이후 처리)
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

        // 6. Save initial delivery history (SENDER)
        deliveryHistoryRepository.save(DeliveryHistory.builder()
                .location("SENDER")
                .currLatitude(order.getCurrLatitude())
                .currLongitude(order.getCurrLongitude())
                .arrivedAt(LocalDateTime.now())
                .delivery(savedDelivery)
                .build());

        return savedDelivery;
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
}
