package com.example.java.admin.repository;

import static com.example.java.product.entity.QProduct.product;
import static com.example.java.product.entity.QOptions.options;
import static com.example.java.product.entity.QSeller.seller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.example.java.admin.dto.AdminProductListDto;
import com.example.java.admin.dto.AdminProductSearchDto;
import com.example.java.product.entity.Product;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
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
				.fetchOne();
		
		return findProduct;
	}

	public Slice<AdminProductListDto> findProducts(AdminProductSearchDto search, Pageable pageable) {
		List<AdminProductListDto> content = queryFactory
				.select(Projections.constructor(AdminProductListDto.class,
						options.seq,
						product.seq,
						product.saleStatus,
						product.productName,
						product.price.add(options.additionalPrice),
						options.color, // Simple representation for optionsName
						product.thumbnailUrl,
						seller.name,
						product.createdDate
				))
				.from(options)
				.join(options.product, product)
				.leftJoin(seller).on(product.sellerSeq.eq(seller.seq))
				.where(
						saleStatusEq(search.getSaleStatus()),
						statusEq(search.getStatus()),
						categoryEq(search.getCategorySeq()),
						priceBetween(search.getPriceFrom(), search.getPriceTo()),
						createdDateBetween(search.getCreatedDateFrom(), search.getCreatedDateTo()),
						keywordContains(search.getSearchType(), search.getKeyword())
				)
				.orderBy(product.createdDate.desc(), options.seq.desc())
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize() + 1)
				.fetch();

		boolean hasNext = false;
		if (content.size() > pageable.getPageSize()) {
			content.remove(pageable.getPageSize());
			hasNext = true;
		}

		return new SliceImpl<>(content, pageable, hasNext);
	}

	private BooleanExpression saleStatusEq(String saleStatus) {
		return StringUtils.hasText(saleStatus) ? product.saleStatus.eq(saleStatus) : null;
	}

	private BooleanExpression statusEq(String status) {
		return StringUtils.hasText(status) ? product.status.eq(status) : null;
	}

	private BooleanExpression categoryEq(Long categorySeq) {
		return categorySeq != null ? product.categorySeq.eq(categorySeq) : null;
	}

	private BooleanExpression priceBetween(Integer priceFrom, Integer priceTo) {
		if (priceFrom != null && priceTo != null) {
			return product.price.add(options.additionalPrice).between(priceFrom, priceTo);
		} else if (priceFrom != null) {
			return product.price.add(options.additionalPrice).goe(priceFrom);
		} else if (priceTo != null) {
			return product.price.add(options.additionalPrice).loe(priceTo);
		}
		return null;
	}

	private BooleanExpression createdDateBetween(LocalDateTime from, LocalDateTime to) {
		if (from != null && to != null) {
			return product.createdDate.between(from, to);
		} else if (from != null) {
			return product.createdDate.goe(from);
		} else if (to != null) {
			return product.createdDate.loe(to);
		}
		return null;
	}

	private BooleanExpression keywordContains(String searchType, String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return null;
		}
		if ("sellerName".equals(searchType)) {
			return seller.name.contains(keyword);
		}
		return product.productName.contains(keyword);
	}
}
