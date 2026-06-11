package com.example.java.orders.repository;

import static com.example.java.admin.hotdeal.Entity.QHotDealProduct.hotDealProduct;
import static com.example.java.cart.entity.QCart.cart;
import static com.example.java.member.entity.QCoupon.coupon;
import static com.example.java.member.entity.QMemberCoupon.memberCoupon;
import static com.example.java.product.entity.QOptions.options;
import static com.example.java.product.entity.QProduct.product;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.java.orders.dto.CheckoutItemDto;
import com.example.java.orders.dto.CouponDto;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrdersQueryRepositoryImpl implements OrdersQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 로그인 회원의 장바구니 중 선택한 cartSeq 목록만 주문/결제 화면용 DTO로 조회한다.
     *
     * 여기서 핫딜 테이블도 같이 조회해서 상품별 핫딜 할인금액을 계산한다.
     */
    @Override
    public List<CheckoutItemDto> findCheckoutItemsByMemberCart(Long memberSeq, List<Long> cartSeqList) {
        if (memberSeq == null || cartSeqList == null || cartSeqList.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();

        List<Tuple> rows = queryFactory
                .select(
                        cart.seq,
                        cart.quantity,

                        options.seq,
                        product.seq,
                        product.productName,
                        product.price,
                        product.thumbnailUrl,
                        options.additionalPrice,

                        hotDealProduct.hotDeal.discountRate,
                        hotDealProduct.hotDeal.discountPrice,

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
                        options.optionsType
                )
                .from(cart)
                .join(cart.options, options)
                .join(options.product, product)

                /*
                    핫딜이 없는 상품도 결제 대상에 포함되어야 하므로 leftJoin 사용.
                    현재 시간이 핫딜 기간 안이고, status = 1인 핫딜만 적용한다.
                 */
                .leftJoin(hotDealProduct).on(
                        hotDealProduct.options.seq.eq(options.seq),
                        hotDealProduct.hotDeal.status.eq(1),
                        hotDealProduct.hotDeal.startDate.loe(now),
                        hotDealProduct.hotDeal.endDate.goe(now)
                )
                .where(
                        cart.member.seq.eq(memberSeq),
                        cart.seq.in(cartSeqList),

                        /*
                            구매 불가 상품 제외

                            product 테이블 기준:
                            - sale_status = SOLD_OUT : 품절
                            - sale_status = STOPPED  : 판매중지
                            - hide_yn = Y            : 숨김 상품
                            - status = DELETED       : 삭제 상품
                        */
                        product.saleStatus.notIn("SOLD_OUT", "STOPPED"),
                        product.hideYn.eq("N"),
                        product.status.ne("DELETED")
                )
                .orderBy(cart.seq.asc())
                .fetch();

        return rows.stream()
                .map(row -> {
                    Integer productPrice = row.get(product.price);
                    Integer additionalPrice = row.get(options.additionalPrice);
                    Integer quantity = row.get(cart.quantity);

                    int originalUnitPrice = nullToZero(productPrice) + nullToZero(additionalPrice);

                    Integer hotdealRate = row.get(hotDealProduct.hotDeal.discountRate);
                    Integer hotdealPrice = row.get(hotDealProduct.hotDeal.discountPrice);

                    int hotdealUnitDiscount =
                            calculateHotdealUnitDiscount(originalUnitPrice, hotdealRate, hotdealPrice);

                    int finalUnitPrice = originalUnitPrice - hotdealUnitDiscount;

                    if (finalUnitPrice < 0) {
                        finalUnitPrice = 0;
                    }

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

                    String imageUrl = row.get(product.thumbnailUrl);

                    if (imageUrl == null || imageUrl.isBlank()) {
                        imageUrl = "/images/no-image.png";
                    }

                    return new CheckoutItemDto(
                            row.get(options.seq),
                            row.get(product.seq),
                            row.get(product.productName),
                            imageUrl,
                            optionText,
                            originalUnitPrice,
                            hotdealUnitDiscount,
                            finalUnitPrice,
                            quantity == null ? 1 : quantity
                    );
                })
                .toList();
    }

    @Override
    public List<CouponDto> findAvailableCouponsByMemberSeq(Long memberSeq) {
        if (memberSeq == null) {
            return List.of();
        }

        LocalDate today = LocalDate.now();

        return queryFactory
                .select(
                        memberCoupon.seq,
                        coupon.name,
                        coupon.discountType,
                        coupon.discountPrice,
                        coupon.discountRate
                )
                .from(memberCoupon)
                .join(memberCoupon.coupon, coupon)
                .where(
                        memberCoupon.member.seq.eq(memberSeq),
                        memberCoupon.status.eq(0),
                        coupon.status.eq(1),
                        coupon.startDate.loe(today),
                        coupon.expireDate.goe(today)
                )
                .orderBy(memberCoupon.seq.desc())
                .fetch()
                .stream()
                .map(row -> {
                    Integer discountType = row.get(coupon.discountType);
                    Integer discountPrice = row.get(coupon.discountPrice);
                    Integer discountRate = row.get(coupon.discountRate);

                    return new CouponDto(
                            row.get(memberCoupon.seq),
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
    public CouponDto findAvailableCouponByMemberSeqAndMemberCouponSeq(Long memberSeq, Long memberCouponSeq) {
        if (memberSeq == null || memberCouponSeq == null) {
            return null;
        }

        LocalDate today = LocalDate.now();

        Tuple row = queryFactory
                .select(
                        memberCoupon.seq,
                        coupon.name,
                        coupon.discountType,
                        coupon.discountPrice,
                        coupon.discountRate
                )
                .from(memberCoupon)
                .join(memberCoupon.coupon, coupon)
                .where(
                        memberCoupon.seq.eq(memberCouponSeq),
                        memberCoupon.member.seq.eq(memberSeq),
                        memberCoupon.status.eq(0),
                        coupon.status.eq(1),
                        coupon.startDate.loe(today),
                        coupon.expireDate.goe(today)
                )
                .fetchOne();

        if (row == null) {
            return null;
        }

        Integer discountType = row.get(coupon.discountType);
        Integer discountPrice = row.get(coupon.discountPrice);
        Integer discountRate = row.get(coupon.discountRate);

        return new CouponDto(
                row.get(memberCoupon.seq),
                row.get(coupon.name),
                discountType,
                discountPrice,
                discountRate,
                makeDiscountText(discountType, discountPrice, discountRate)
        );
    }

    private int calculateHotdealUnitDiscount(int originalUnitPrice,
                                             Integer discountRate,
                                             Integer discountPrice) {
        if (originalUnitPrice <= 0) {
            return 0;
        }

        int discount = 0;

        /*
            discount_price가 있으면 정액 할인 우선 적용.
            discount_price가 없고 discount_rate가 있으면 정률 할인 적용.
         */
        if (discountPrice != null && discountPrice > 0) {
            discount = discountPrice;
        } else if (discountRate != null && discountRate > 0) {
            discount = originalUnitPrice * discountRate / 100;
        }

        if (discount < 0) {
            return 0;
        }

        return Math.min(originalUnitPrice, discount);
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