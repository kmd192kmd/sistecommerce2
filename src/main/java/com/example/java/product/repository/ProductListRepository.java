package com.example.java.product.repository;

import static com.example.java.product.entity.QProduct.product;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.java.product.entity.Product;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/*
    ProductListRepository

    상품 목록 조회, 필터링, 검색 등 읽기 전용 목록 관련 DB 작업을 담당합니다.
*/
@Repository
@RequiredArgsConstructor
public class ProductListRepository {

    private final JPAQueryFactory queryFactory;

    /*
        상품 필터 검색 (QueryDSL 사용)
    */
    public Page<Product> findWithFilters(
            List<Long> categorySeqs,
            String keyword,
            Integer minPrice,
            Integer maxPrice,
            Double minRating,
            String saleStatus,
            Pageable pageable) {

        BooleanExpression condition = product.hideYn.eq("N")
                .and(product.saleStatus.ne("STOPPED"))
                .and(product.status.eq("NORMAL"));

        if (categorySeqs != null && !categorySeqs.isEmpty()) {
            condition = condition.and(product.categorySeq.in(categorySeqs));
        }

        if (keyword != null && !keyword.isBlank()) {
            String cleanKeyword = keyword.replace(" ", "").toLowerCase();
            condition = condition.and(
                Expressions.stringTemplate("REPLACE(LOWER({0}), ' ', '')", product.productName)
                           .contains(cleanKeyword)
            );
        }

        if (minPrice != null) {
            condition = condition.and(product.price.goe(minPrice));
        }

        if (maxPrice != null) {
            condition = condition.and(product.price.loe(maxPrice));
        }

        if (minRating != null) {
            condition = condition.and(product.avgRating.goe(minRating));
        }

        if (saleStatus != null && !saleStatus.isBlank()) {
            condition = condition.and(product.saleStatus.eq(saleStatus));
        }

        List<Product> products = queryFactory
                .selectFrom(product)
                .where(condition)
                .orderBy(getOrderSpecifier(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(product.count())
                .from(product)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(
                products,
                pageable,
                totalCount == null ? 0 : totalCount
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
	private OrderSpecifier<?> getOrderSpecifier(org.springframework.data.domain.Sort sort) {
        if (sort.isUnsorted()) {
            return product.seq.desc();
        }
        PathBuilder<Product> entityPath = new PathBuilder<>(Product.class, "product");
        for (org.springframework.data.domain.Sort.Order order : sort) {
            if ("recommend".equals(order.getProperty())) {
                com.querydsl.core.types.dsl.NumberExpression<Double> recommendScore = 
                    product.salesCount.doubleValue().multiply(50.0)
                    .add(product.viewCount.doubleValue().multiply(30.0))
                    .add(product.avgRating.multiply(10.0))
                    .add(product.reviewCount.doubleValue().multiply(10.0));
                return new OrderSpecifier(com.querydsl.core.types.Order.DESC, recommendScore);
            }
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            return new OrderSpecifier(direction, entityPath.get(order.getProperty()));
        }
        return product.seq.desc();
    }
}
