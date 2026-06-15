package com.example.java.statistics.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.java.statistics.dto.PieChartDTO;
import com.example.java.statistics.dto.SalesRatingDTO;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StatisticsRepository {
	private final EntityManager em;

	public List<Object[]> getDailySales(int days) {

	    String sql = """
	        SELECT
	            TO_CHAR(TRUNC(pay_date), 'YYYY-MM-DD') label,
	            SUM(amount) sales
	        FROM payment
	        WHERE status = 2
	          AND pay_date >= TRUNC(SYSDATE) - :days
	        GROUP BY TRUNC(pay_date)
	        ORDER BY TRUNC(pay_date)
	        """;

	    return em.createNativeQuery(sql)
	            .setParameter("days", days)
	            .getResultList();
	}
	
	public List<Object[]> getMonthlySales(int months) {

	    String sql = """
	        SELECT
	            TO_CHAR(pay_date, 'YYYY-MM') label,
	            SUM(amount) sales
	        FROM payment
	        WHERE status = 2
	          AND pay_date >= ADD_MONTHS(TRUNC(SYSDATE), - :months)
	        GROUP BY TO_CHAR(pay_date, 'YYYY-MM')
	        ORDER BY label
	        """;

	    return em.createNativeQuery(sql)
	            .setParameter("months", months)
	            .getResultList();
	}
	
	// 상품별 판매량 TOP5
    public List<SalesRatingDTO> getTopProductSales() {

        String sql = """
                SELECT
                    p.product_name,
                    p.sales_count
                FROM product p
                ORDER BY p.sales_count DESC, p.seq
                FETCH FIRST 5 ROWS ONLY
                """;

        List<Object[]> result = em.createNativeQuery(sql)
                .getResultList();
        return result.stream()
                .map(row -> new SalesRatingDTO(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    // 카테고리별 판매량 TOP5
    public List<SalesRatingDTO> getTopCategorySales() {

        String sql = """
                SELECT
                    c.category_name,
                    SUM(p.sales_count) AS total_sales
                FROM product p
                JOIN category c
                    ON p.category_seq = c.seq
                GROUP BY c.category_name
                ORDER BY total_sales DESC
                FETCH FIRST 5 ROWS ONLY
                """;

        List<Object[]> result = em.createNativeQuery(sql)
                .getResultList();
        return result.stream()
                .map(row -> new SalesRatingDTO(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    // 판매처별 판매량 TOP5
    public List<SalesRatingDTO> getTopSellerSales() {

        String sql = """
                SELECT
                    s.name,
                    SUM(p.sales_count) AS total_sales
                FROM product p
                JOIN seller s
                    ON p.seller_seq = s.seq
                GROUP BY s.name
                ORDER BY total_sales DESC
                FETCH FIRST 5 ROWS ONLY
                """;

        List<Object[]> result = em.createNativeQuery(sql)
                .getResultList();
        return result.stream()
                .map(row -> new SalesRatingDTO(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }
    
    public List<PieChartDTO> getProductStatusStatistics() {

        String sql = """
                SELECT
                    p.sale_status,
                    COUNT(*)
                FROM product p
                GROUP BY p.sale_status
                ORDER BY COUNT(*) DESC
                """;

        List<Object[]> result =
                em.createNativeQuery(sql)
                        .getResultList();

        return result.stream()
                .map(row -> new PieChartDTO(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }
    public List<PieChartDTO> getDeliveryStatusStatistics() {

        String sql = """
                SELECT
                    d.status,
                    COUNT(*)
                FROM delivery d
                GROUP BY d.status
                ORDER BY COUNT(*) DESC
                """;

        List<Object[]> result =
                em.createNativeQuery(sql)
                        .getResultList();

        return result.stream()
                .map(row -> new PieChartDTO(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }
}
