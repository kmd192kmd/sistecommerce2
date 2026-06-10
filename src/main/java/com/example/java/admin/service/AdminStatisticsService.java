package com.example.java.admin.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.java.admin.dto.AdminChartDto;
import com.example.java.admin.dto.AdminStatisticsDto;
import com.example.java.admin.repository.AdminAppPaymentRepository;
import com.example.java.admin.repository.AdminOrderItemRepository;
import com.example.java.orders.entity.Payment;
import com.example.java.orders.entity.OrderItem;
import com.example.java.purchaseorder.entity.PurchaseOrder;
import com.example.java.purchaseorder.repository.PurchaseOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final AdminAppPaymentRepository paymentRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final AdminOrderItemRepository adminOrderItemRepository;

    public AdminStatisticsDto getStatistics() {
        List<Payment> payments = paymentRepository.findByStatus(2);
        long totalRevenue = payments.stream()
                .filter(p -> p.getAmount() != null)
                .mapToLong(Payment::getAmount).sum();

        List<OrderItem> returnedItems = adminOrderItemRepository.findByItemStatusIn(Arrays.asList(8, 9));
        long totalRefund = returnedItems.stream()
                .filter(item -> item.getSubTotalPrice() != null)
                .mapToLong(OrderItem::getSubTotalPrice).sum();

        totalRevenue -= totalRefund;

        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll();
        long totalPayout = purchaseOrders.stream()
                .filter(po -> po.getStatus() == com.example.java.purchaseorder.enums.PurchaseOrderStatus.입고완료)
                .filter(po -> po.getTotalPrice() != null)
                .mapToLong(PurchaseOrder::getTotalPrice).sum();

        long netProfit = totalRevenue - totalPayout;
        return AdminStatisticsDto.builder()
                .totalRevenue(totalRevenue)
                .totalPayout(totalPayout)
                .netProfit(netProfit)
                .build();
    }

    public AdminChartDto getChartData(int days) {
        List<Payment> payments = paymentRepository.findByStatus(2);
        List<OrderItem> returnedItems = adminOrderItemRepository.findByItemStatusIn(Arrays.asList(8, 9));

        LocalDate today = LocalDate.now();
        List<String> dateLabels = new ArrayList<>();
        List<Long> dailyRevenues = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dateLabels.add(date.toString());
            
            long dailyRev = payments.stream()
                .filter(p -> p.getAmount() != null)
                .filter(p -> p.getPayDate() != null && p.getPayDate().toLocalDate().equals(date))
                .mapToLong(Payment::getAmount)
                .sum();
                
            // 대략적으로 같은 날짜의 주문에서 발생한 반품을 차감 (정확한 반품 승인일자가 없으므로 주문일/결제일 기준)
            long dailyRefund = returnedItems.stream()
                .filter(item -> item.getSubTotalPrice() != null)
                .filter(item -> {
                    Payment p = payments.stream().filter(pay -> pay.getOrderSeq().equals(item.getOrderSeq())).findFirst().orElse(null);
                    return p != null && p.getPayDate() != null && p.getPayDate().toLocalDate().equals(date);
                })
                .mapToLong(OrderItem::getSubTotalPrice).sum();

            dailyRevenues.add(dailyRev - dailyRefund);
        }
        
        return new AdminChartDto(dateLabels, dailyRevenues);
    }
}
