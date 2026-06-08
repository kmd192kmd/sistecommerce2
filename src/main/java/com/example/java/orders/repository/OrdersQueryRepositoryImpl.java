package com.example.java.orders.repository;

import static com.example.java.member.entity.QCoupon.coupon;
import static com.example.java.product.entity.QOptions.options;
import static com.example.java.product.entity.QProduct.product;
import static com.example.java.product.entity.QProductImage.productImage;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.example.java.orders.dto.CheckoutItemDto;
import com.example.java.orders.dto.CouponDto;
import com.example.java.product.entity.QProductImage;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrdersQueryRepositoryImpl implements OrdersQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final List<Long> TEST_OPTIONS_SEQS = List.of(2158L, 2159L);

    private static final Map<Long, Integer> TEST_QUANTITY_MAP = Map.of(
            2158L, 1,
            2159L, 2
    );

    private static final long TEST_RATE_COUPON_SEQ = 80L;
    private static final long TEST_PRICE_COUPON_SEQ = 81L;

    @Override
    public List<CheckoutItemDto> findCheckoutItemsByTestOptionsSeq() {

        QProductImage subImage = new QProductImage("subImage");

        List<Tuple> rows = queryFactory
                .select(
                        options.seq,
                        product.seq,
                        product.productName,
                        product.price,
                        options.additionalPrice,
                        options.color,
                        options.optionsSize,
                        options.volumeWeight,
                        options.taste,
                        options.storageType,
                        options.scentIngredient,
                        options.voltage,
                        options.quantitySet,
                        options.sizeSpec,
                        options.storageCapacity,
                        options.memory,
                        options.switchAxis,
                        options.connectionType,
                        options.wearableSpec,
                        options.materialType,
                        options.optionsType,
                        productImage.imageUrl
                )
                .from(options)
                .join(product).on(options.productSeq.eq(product.seq))
                .leftJoin(productImage).on(
                        productImage.productSeq.eq(product.seq)
                                .and(productImage.status.eq("NORMAL"))
                                .and(productImage.seq.eq(
                                        JPAExpressions
                                                .select(subImage.seq.min())
                                                .from(subImage)
                                                .where(
                                                        subImage.productSeq.eq(product.seq),
                                                        subImage.status.eq("NORMAL")
                                                )
                                ))
                )
                .where(options.seq.in(TEST_OPTIONS_SEQS))
                .orderBy(options.seq.asc())
                .fetch();

        return rows.stream()
                .map(row -> {
                    Long optionsSeq = row.get(options.seq);

                    Integer productPrice = row.get(product.price);
                    Integer additionalPrice = row.get(options.additionalPrice);

                    int finalUnitPrice = nullToZero(productPrice) + nullToZero(additionalPrice);

                    int quantity = TEST_QUANTITY_MAP.getOrDefault(optionsSeq, 1);

                    String optionText = buildOptionText(
                            row.get(options.color),
                            row.get(options.optionsSize),
                            row.get(options.volumeWeight),
                            row.get(options.taste),
                            row.get(options.storageType),
                            row.get(options.scentIngredient),
                            row.get(options.voltage),
                            row.get(options.quantitySet),
                            row.get(options.sizeSpec),
                            row.get(options.storageCapacity),
                            row.get(options.memory),
                            row.get(options.switchAxis),
                            row.get(options.connectionType),
                            row.get(options.wearableSpec),
                            row.get(options.materialType),
                            row.get(options.optionsType)
                    );

                    String imageUrl = row.get(productImage.imageUrl);

                    if (imageUrl == null || imageUrl.isBlank()) {
                        imageUrl = "/images/no-image.png";
                    }

                    return new CheckoutItemDto(
                            optionsSeq,
                            row.get(product.seq),
                            row.get(product.productName),
                            imageUrl,
                            optionText,
                            finalUnitPrice,
                            quantity
                    );
                })
                .toList();
    }

    @Override
    public List<CouponDto> findTestCoupons() {
        return queryFactory
                .select(
                        coupon.seq,
                        coupon.name,
                        coupon.discountType,
                        coupon.discountPrice,
                        coupon.discountRate
                )
                .from(coupon)
                .where(
                        coupon.seq.in(TEST_RATE_COUPON_SEQ, TEST_PRICE_COUPON_SEQ),
                        coupon.status.eq(1)
                )
                .orderBy(coupon.seq.asc())
                .fetch()
                .stream()
                .map(row -> {
                    Integer discountType = row.get(coupon.discountType);
                    Integer discountPrice = row.get(coupon.discountPrice);
                    Integer discountRate = row.get(coupon.discountRate);

                    return new CouponDto(
                            row.get(coupon.seq),
                            row.get(coupon.name),
                            discountType,
                            discountPrice,
                            discountRate,
                            makeDiscountText(discountType, discountPrice, discountRate)
                    );
                })
                .toList();
    }

    @Override
    public CouponDto findTestCoupon(Long couponSeq) {
        if (couponSeq == null) {
            return null;
        }

        Tuple row = queryFactory
                .select(
                        coupon.seq,
                        coupon.name,
                        coupon.discountType,
                        coupon.discountPrice,
                        coupon.discountRate
                )
                .from(coupon)
                .where(
                        coupon.seq.eq(couponSeq),
                        coupon.seq.in(TEST_RATE_COUPON_SEQ, TEST_PRICE_COUPON_SEQ),
                        coupon.status.eq(1)
                )
                .fetchOne();

        if (row == null) {
            return null;
        }

        Integer discountType = row.get(coupon.discountType);
        Integer discountPrice = row.get(coupon.discountPrice);
        Integer discountRate = row.get(coupon.discountRate);

        return new CouponDto(
                row.get(coupon.seq),
                row.get(coupon.name),
                discountType,
                discountPrice,
                discountRate,
                makeDiscountText(discountType, discountPrice, discountRate)
        );
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String buildOptionText(String... values) {
        StringBuilder sb = new StringBuilder();

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" / ");
                }
                sb.append(value);
            }
        }

        if (sb.isEmpty()) {
            return "기본 옵션";
        }

        return sb.toString();
    }

    private String makeDiscountText(Integer discountType, Integer discountPrice, Integer discountRate) {
        if (discountType == null) {
            return "할인정보 없음";
        }

        if (discountType == 0) {
            return discountRate == null ? "할인율 없음" : discountRate + "% 할인";
        }

        if (discountType == 1) {
            return discountPrice == null ? "할인금액 없음" : String.format("%,d원 할인", discountPrice);
        }

        return "알 수 없는 할인";
    }
}