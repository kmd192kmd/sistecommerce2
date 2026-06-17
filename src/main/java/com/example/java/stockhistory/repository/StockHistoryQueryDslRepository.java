package com.example.java.stockhistory.repository;

import static com.example.java.product.entity.QOptions.options;
import static com.example.java.product.entity.QProduct.product;
import static com.example.java.stockhistory.entity.QStockHistory.stockHistory;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.example.java.stockhistory.dto.StockHistorySearchDTO;
import com.example.java.stockhistory.entity.StockHistory;
import com.example.java.stockhistory.enums.StockHistoryType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StockHistoryQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    public List<StockHistory> findAllWithSearchCond(
            StockHistorySearchDTO search,
            Pageable pageable
    ) {

        return queryFactory
                .selectFrom(stockHistory)
                .join(stockHistory.options, options).fetchJoin()
                .join(options.product, product).fetchJoin()
                .where(
                        saleStatusEq(search.getSaleStatus()),
                        stockHistoryTypeEq(search.getType()),
                        priceGoe(search.getPriceFrom()),
                        priceLoe(search.getPriceTo()),
                        changedDateGoe(search.getChangedDateFrom()),
                        changedDateLoe(search.getChangedDateTo()),
                        productNameContains(search.getKeyword())
                )
                .orderBy(stockHistory.seq.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();
    }

    // ========================
    // 조건 메서드들
    // ========================
    private BooleanExpression saleStatusEq(String saleStatus) {
        if (!StringUtils.hasText(saleStatus)) {
            return null;
        }
        return product.saleStatus.eq(saleStatus);
    }

    private BooleanExpression stockHistoryTypeEq(StockHistoryType type) {
        return type == null ? null : stockHistory.type.eq(type);
    }

    private BooleanExpression priceGoe(Long priceFrom) {
        return priceFrom == null ? null : product.price.goe(priceFrom);
    }

    private BooleanExpression priceLoe(Long priceTo) {
        return priceTo == null ? null : product.price.loe(priceTo);
    }

    private BooleanExpression changedDateGoe(LocalDateTime from) {
        return from == null ? null : stockHistory.createdAt.goe(from);
    }

    private BooleanExpression changedDateLoe(LocalDateTime to) {
        return to == null ? null : stockHistory.createdAt.loe(to);
    }

    private BooleanExpression productNameContains(String keyword) {
        return StringUtils.hasText(keyword)
                ? product.productName.containsIgnoreCase(keyword)
                : null;
    }
}