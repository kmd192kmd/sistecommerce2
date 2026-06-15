package com.example.java.admin.repository;

import static com.example.java.product.entity.QOptions.options;
import static com.example.java.product.entity.QProduct.product;
import static com.example.java.product.entity.QSeller.seller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.java.admin.dto.GroupBuyAdminListDto;
import com.example.java.admin.dto.GroupBuySearchDto;
import com.example.java.admin.dto.SellerInfoDto;
import com.example.java.groupbuy.entity.GroupBuyStatus;
import com.example.java.groupbuy.entity.ParticipationStatus;
import com.example.java.groupbuy.entity.QGroupBuy;
import com.example.java.groupbuy.entity.QParticipation;
import com.example.java.groupbuy.entity.QWaitingQueue;
import com.example.java.product.entity.Options;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

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

    // --- 추가하는 동적 조회 메서드 ---
    public Page<GroupBuyAdminListDto> searchGroupBuys(GroupBuySearchDto searchDto, Pageable pageable) {
        QGroupBuy groupBuy = QGroupBuy.groupBuy;
        QParticipation participation = QParticipation.participation;
        QWaitingQueue waitingQueue = QWaitingQueue.waitingQueue;

        // 참여 완료(PARTICIPATING) 인원 카운트 서브쿼리
        var participantCountSub = JPAExpressions.select(participation.count())
                .from(participation)
                .where(participation.groupBuy.eq(groupBuy)
                        .and(participation.status.eq(ParticipationStatus.PARTICIPATING)));

        // 대기자 수 카운트 서브쿼리
        var waitingCountSub = JPAExpressions.select(waitingQueue.count())
                .from(waitingQueue)
                .where(waitingQueue.groupBuy.eq(groupBuy));

        List<GroupBuyAdminListDto> content = queryFactory
                .select(Projections.fields(GroupBuyAdminListDto.class,
                        groupBuy.seq,
                        groupBuy.status,
                        seller.name.as("sellerName"),
                        product.productName.as("productName"),
                        ExpressionUtils.as(participantCountSub, "participantCount"),
                        groupBuy.minCount,
                        ExpressionUtils.as(waitingCountSub, "waitingCount"),
                        groupBuy.startAt,
                        groupBuy.endAt
                ))
                .from(groupBuy)
                .join(groupBuy.product, product)
                .join(seller).on(seller.seq.eq(product.sellerSeq))
                .where(
                        statusEq(searchDto.getStatus()),
                        participantMinGoe(searchDto.getParticipantMin(), groupBuy, participation),
                        participantMaxLoe(searchDto.getParticipantMax(), groupBuy, participation),
                        startAtBetween(searchDto.getStartAtFrom(), searchDto.getStartAtTo(), groupBuy),
                        endAtBetween(searchDto.getEndAtFrom(), searchDto.getEndAtTo(), groupBuy),
                        keywordLike(searchDto.getSearchType(), searchDto.getKeyword())
                )
                .orderBy(groupBuy.seq.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 매칭 데이터 수 계산
        Long total = queryFactory
                .select(groupBuy.count())
                .from(groupBuy)
                .join(groupBuy.product, product)
                .join(seller).on(seller.seq.eq(product.sellerSeq))
                .where(
                        statusEq(searchDto.getStatus()),
                        participantMinGoe(searchDto.getParticipantMin(), groupBuy, participation),
                        participantMaxLoe(searchDto.getParticipantMax(), groupBuy, participation),
                        startAtBetween(searchDto.getStartAtFrom(), searchDto.getStartAtTo(), groupBuy),
                        endAtBetween(searchDto.getEndAtFrom(), searchDto.getEndAtTo(), groupBuy),
                        keywordLike(searchDto.getSearchType(), searchDto.getKeyword())
                )
                .fetchOne();

        if (total == null) {
            total = 0L;
        }

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression statusEq(GroupBuyStatus status) {
        return status != null ? QGroupBuy.groupBuy.status.eq(status) : null;
    }

    private BooleanExpression participantMinGoe(Integer min, QGroupBuy groupBuy, QParticipation participation) {
        if (min == null) return null;
        return JPAExpressions.select(participation.count())
                .from(participation)
                .where(participation.groupBuy.eq(groupBuy)
                        .and(participation.status.eq(ParticipationStatus.PARTICIPATING)))
                .goe(min.longValue());
    }

    private BooleanExpression participantMaxLoe(Integer max, QGroupBuy groupBuy, QParticipation participation) {
        if (max == null) return null;
        return JPAExpressions.select(participation.count())
                .from(participation)
                .where(participation.groupBuy.eq(groupBuy)
                        .and(participation.status.eq(ParticipationStatus.PARTICIPATING)))
                .loe(max.longValue());
    }

    private BooleanExpression startAtBetween(LocalDateTime from, LocalDateTime to, QGroupBuy groupBuy) {
        if (from == null && to == null) return null;
        if (from == null) return groupBuy.startAt.loe(to);
        if (to == null) return groupBuy.startAt.goe(from);
        return groupBuy.startAt.between(from, to);
    }

    private BooleanExpression endAtBetween(LocalDateTime from, LocalDateTime to, QGroupBuy groupBuy) {
        if (from == null && to == null) return null;
        if (from == null) return groupBuy.endAt.loe(to);
        if (to == null) return groupBuy.endAt.goe(from);
        return groupBuy.endAt.between(from, to);
    }

    private BooleanExpression keywordLike(String searchType, String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        if ("PRODUCT_NAME".equals(searchType)) {
            return product.productName.containsIgnoreCase(keyword);
        } else if ("SELLER_NAME".equals(searchType)) {
            return seller.name.containsIgnoreCase(keyword);
        }
        return null;
    }
}