package com.example.java.purchaseorder.repository;

import static com.example.java.product.entity.QOptions.options;
import static com.example.java.product.entity.QProduct.product;
import static com.example.java.purchaseorder.entity.QPurchaseOrder.purchaseOrder;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.example.java.purchaseorder.dto.PurchaseOrderSearchDTO;
import com.example.java.purchaseorder.entity.PurchaseOrder;
import com.example.java.purchaseorder.enums.PurchaseOrderStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PurchaseOrderQueryDslRepository {

	private final JPAQueryFactory queryFactory;
	
	public List<PurchaseOrder> findAllWithOptionsAndProduct(
			 PurchaseOrderSearchDTO search,
			 Pageable pageable) {

	    return queryFactory
	        .selectFrom(purchaseOrder)
	        .join(purchaseOrder.options, options).fetchJoin()
	        .join(options.product, product).fetchJoin()
	        .where(
	            statusEq(search.getStatus()),
	            totalPriceGoe(search.getTotalPriceFrom()),
	            totalPriceLoe(search.getTotalPriceTo()),
	            orderDateGoe(search.getOrderDateFrom()),
	            orderDateLoe(search.getOrderDateTo()),
	            expectedDateGoe(search.getExpectedDateFrom()),
	            expectedDateLoe(search.getExpectedDateTo()),
	            productNameContains(search.getKeyword())
	        )
	        .orderBy(purchaseOrder.seq.desc())
	        .offset(pageable.getOffset())
	        .limit(pageable.getPageSize() + 1)
	        .fetch();
    }
	 
	 
	private BooleanExpression totalPriceGoe(Long totalPriceFrom) {
	    return totalPriceFrom == null
	            ? null
	            : purchaseOrder.totalPrice.goe(totalPriceFrom);
	}
	private BooleanExpression totalPriceLoe(Long totalPriceTo) {
	    return totalPriceTo == null
	            ? null
	            : purchaseOrder.totalPrice.loe(totalPriceTo);
	}
	private BooleanExpression statusEq(PurchaseOrderStatus status) {
	    return status == null ? null : purchaseOrder.status.eq(status);
	}
	private BooleanExpression orderDateGoe(LocalDate from) {
	    return from == null ? null : purchaseOrder.orderDate.goe(from);
	}
	private BooleanExpression orderDateLoe(
	        LocalDate to) {

	    return to == null
	            ? null
	            : purchaseOrder.orderDate.loe(to);
	}
	private BooleanExpression expectedDateGoe(LocalDate from) {
	    return from == null
	            ? null
	            : purchaseOrder.expectedDate.goe(from);
	}
	private BooleanExpression expectedDateLoe(LocalDate to) {
	    return to == null
	            ? null
	            : purchaseOrder.expectedDate.loe(to);
	}
	private BooleanExpression productNameContains(String keyword) {
	    return StringUtils.hasText(keyword)
	            ? product.productName.contains(keyword)
	            : null;
	}
	
}
