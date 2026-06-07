package com.example.java.purchaseorder.repository;

import static com.example.java.product.entity.QOptions.options;
import static com.example.java.product.entity.QProduct.product;
import static com.example.java.purchaseorder.entity.QPurchaseOrder.purchaseOrder;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.java.purchaseorder.entity.PurchaseOrder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PurchaseOrderQueryDslRepository {

	private final JPAQueryFactory queryFactory;
	
	 public List<PurchaseOrder> findAllWithOptionsAndProduct(Pageable pageable) {

	        return queryFactory
	                .selectFrom(purchaseOrder)
	                .join(purchaseOrder.options, options).fetchJoin()
	                .join(options.product, product).fetchJoin()
	                .orderBy(purchaseOrder.seq.desc())
	                .offset(pageable.getOffset())
	                .limit(pageable.getPageSize() + 1)
	                .fetch();
	    }
}
