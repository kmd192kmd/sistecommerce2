package com.example.java.admin.repository;

import static com.example.java.product.entity.QProduct.product;

import org.springframework.stereotype.Repository;

import com.example.java.product.entity.Product;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AdminProductQueryRepository {
	
	private final JPAQueryFactory queryFactory;
	
	public Product QueryDslTest(Long productSeq) {
		
		Product findProduct = queryFactory
				.selectFrom(product)
				.where(product.seq.eq(productSeq))
//				.join(product.categorySeq)
				.fetchOne();
		
		return findProduct;
	}
}
