package com.example.java.statistics.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.repository.MemberRepository;
import com.example.java.orders.repository.OrdersRepository;
import com.example.java.product.repository.ProductDetailRepository;
import com.example.java.statistics.dto.MainDashboardDTO;
import com.example.java.statistics.dto.PieChartDTO;
import com.example.java.statistics.dto.SalesRatingDTO;
import com.example.java.statistics.dto.SalesTrendDTO;
import com.example.java.statistics.repository.StatisticsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

	private final StatisticsRepository statisticsRepository;
    private final MemberRepository memberRepository;
    private final ProductDetailRepository productDetailRepository;
    private final OrdersRepository ordersRepository;

    public MainDashboardDTO getDashboard() {
        return MainDashboardDTO.builder()
                .memberTotalCount(memberRepository.count())
                .productTotalCount(Long.valueOf(productDetailRepository.countProducts()))
                .ordersTotalCount(ordersRepository.count())
                .build();
    }

    public List<SalesTrendDTO> getSalesTrend(String period) {

        return switch (period) {
            case "7d" -> getDailyTrend(7);
            case "15d" -> getDailyTrend(15);
            case "month" -> getThisMonthTrend();
            case "3m" -> getMonthlyTrend(3);
            default -> getDailyTrend(7);
        };
    }

    private List<SalesTrendDTO> getDailyTrend(int days) {

        List<Object[]> rows = statisticsRepository.getDailySales(days);
        Map<String, Long> salesMap = new HashMap<>();
        for (Object[] row : rows) {
            salesMap.put(
                    (String) row[0],
                    ((Number) row[1]).longValue()
            );
        }

        List<SalesTrendDTO> result = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String label = date.toString();
            result.add(
                    new SalesTrendDTO(
                            label,
                            salesMap.getOrDefault(label, 0L)
                    )
            );
        }

        return result;
    }

    private List<SalesTrendDTO> getThisMonthTrend() {

        int dayOfMonth = LocalDate.now().getDayOfMonth();
        List<Object[]> rows = statisticsRepository.getDailySales(dayOfMonth);
        Map<String, Long> salesMap = new HashMap<>();

        for (Object[] row : rows) {
            salesMap.put(
                    (String) row[0],
                    ((Number) row[1]).longValue()
            );
        }

        List<SalesTrendDTO> result = new ArrayList<>();

        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        for (LocalDate date = startDate;
             !date.isAfter(today);
             date = date.plusDays(1)) {

            String label = date.toString();

            result.add(
                    new SalesTrendDTO(
                            label,
                            salesMap.getOrDefault(label, 0L)
                    )
            );
        }

        return result;
    }

    private List<SalesTrendDTO> getMonthlyTrend(int months) {

        List<Object[]> rows = statisticsRepository.getMonthlySales(months);
        Map<String, Long> salesMap = new HashMap<>();

        for (Object[] row : rows) {
            salesMap.put(
                    (String) row[0],
                    ((Number) row[1]).longValue()
            );
        }

        List<SalesTrendDTO> result = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {

            YearMonth target = current.minusMonths(i);
            String label = target.toString(); // 2026-06
            result.add(
                    new SalesTrendDTO(
                            label,
                            salesMap.getOrDefault(label, 0L)
                    )
            );
        }

        return result;
    }
    
    public List<SalesRatingDTO> getTopProductSales() {
        return statisticsRepository.getTopProductSales();
    }
    public List<SalesRatingDTO> getTopCategorySales() {
        return statisticsRepository.getTopCategorySales();
    }
    public List<SalesRatingDTO> getTopSellerSales() {
        return statisticsRepository.getTopSellerSales();
    }
    
    public List<PieChartDTO> getProductStatusStatistics() {
    	return statisticsRepository.getProductStatusStatistics()
            .stream()
            .map(dto -> {
                String label = switch (dto.getLabel()) {
                    case "ON_SALE" -> "판매중";
                    case "SOLD_OUT" -> "품절";
                    case "STOPPED" -> "판매중지";
                    default -> dto.getLabel();
                };
                return new PieChartDTO(
                        label,
                        dto.getValue()
                );
            })
            .toList();
    }
    public List<PieChartDTO> getDeliveryStatusStatistics() {
        return statisticsRepository.getDeliveryStatusStatistics()
    		 .stream()
             .map(dto -> {

                 String label = switch (dto.getLabel()) {
                     case "READY" -> "배송준비";
                     case "CANCELED" -> "배송취소";
                     case "SHIPPING" -> "배송중";
                     case "DELIVERED" -> "배송완료";
                     default -> dto.getLabel();
                 };
                 return new PieChartDTO(
                         label,
                         dto.getValue()
                 );
             })
             .toList();	
    }
}