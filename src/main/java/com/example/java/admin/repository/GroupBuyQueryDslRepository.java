package com.example.java.admin.repository;

import static com.example.java.product.entity.QOptions.options;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.java.admin.dto.SellerInfoDto;
import com.example.java.product.entity.Options;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

import static com.example.java.product.entity.QProduct.product;
import static com.example.java.product.entity.QSeller.seller;

@Repository
@RequiredArgsConstructor
public class GroupBuyQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    public List<Options> getOptions(List<Long> optionSeqs) {
        return queryFactory
                .selectFrom(options)
                .join(options.product, product).fetchJoin()
                .where(options.seq.in(optionSeqs))
                .fetch();
    }

    public SellerInfoDto getSellerInfo(Long sellerSeq) {
        return queryFactory
                .select(Projections.constructor(
                        SellerInfoDto.class,
                        seller.name,
                        seller.phone,
                        seller.supplyRate
                ))
                .from(seller)
                .where(seller.seq.eq(sellerSeq))
                .fetchOne();
    }
}