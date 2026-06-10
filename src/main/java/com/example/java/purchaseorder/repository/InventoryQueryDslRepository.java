package com.example.java.purchaseorder.repository;

import static com.example.java.product.entity.QOptions.options;
import static com.example.java.product.entity.QProduct.product;
import static com.example.java.product.entity.QSeller.seller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.example.java.product.entity.Options;
import com.example.java.product.entity.Product;
import com.example.java.purchaseorder.dto.InventoryListDTO;
import com.example.java.purchaseorder.dto.InventorySearchDTO;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InventoryQueryDslRepository {

	private final JPAQueryFactory queryFactory;
	
	public List<InventoryListDTO> findAllWithSearchCond(
	        InventorySearchDTO search,
	        Pageable pageable
	) {

	    List<Tuple> results = queryFactory
	            .select(options, seller.name)
	            .from(options)
	            .join(options.product, product).fetchJoin()
	            .join(seller)
	                .on(product.sellerSeq.eq(seller.seq))
	            .where(
	                    saleStatusEq(search.getSaleStatus()),
	                    underSafetyStock(search.getStockType()),
	                    priceGoe(search.getPriceFrom()),
	                    priceLoe(search.getPriceTo()),
	                    createdDateGoe(search.getCreatedDateFrom()),
	                    createdDateLoe(search.getCreatedDateTo()),
	                    productNameContains(search.getKeyword())
	            )
	            .orderBy(options.seq.desc())
	            .offset(pageable.getOffset())
	            .limit(pageable.getPageSize() + 1)
	            .fetch();

	    return results.stream()
	            .map(tuple -> {

	                Options option = tuple.get(options);
	                Product product = option.getProduct();

	                return InventoryListDTO.builder()
	                        .optionsSeq(option.getSeq())
	                        .saleStatus(product.getSaleStatus())
	                        .productName(product.getProductName())
	                        .price(product.getPrice() + option.getAdditionalPrice())
	                        .optionsName(option.getDisplayName())
	                        .stock(option.getStock())
	                        .safetyStock(option.getSafetyStock())
	                        .sellerName(tuple.get(seller.name))
	                        .createdDate(product.getCreatedDate())
	                        .build();
	            })
	            .toList();
	}
	
	private BooleanExpression saleStatusEq(String saleStatus) {

	    return StringUtils.hasText(saleStatus)
	            ? product.saleStatus.eq(saleStatus)
	            : null;
	}
	private BooleanExpression underSafetyStock(String stockType) {

	    if (!"UNDER_SAFE".equals(stockType)) {
	        return null;
	    }

	    return options.stock.loe(options.safetyStock);
	}
	private BooleanExpression priceGoe(Integer priceFrom) {

	    if (priceFrom == null) {
	        return null;
	    }

	    return product.price
	            .add(options.additionalPrice)
	            .goe(priceFrom);
	}
	private BooleanExpression priceLoe(Integer priceTo) {

	    if (priceTo == null) {
	        return null;
	    }

	    return product.price
	            .add(options.additionalPrice)
	            .loe(priceTo);
	}
	private BooleanExpression createdDateGoe(LocalDateTime from) {

	    return from == null
	            ? null
	            : product.createdDate.goe(from);
	}
	private BooleanExpression createdDateLoe(LocalDateTime to) {

	    return to == null
	            ? null
	            : product.createdDate.loe(to);
	}
	private BooleanExpression productNameContains(String keyword) {

	    return StringUtils.hasText(keyword)
	            ? product.productName.containsIgnoreCase(keyword)
	            : null;
	}
}
