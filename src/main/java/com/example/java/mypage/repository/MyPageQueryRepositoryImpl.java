package com.example.java.mypage.repository;

import static com.example.java.orders.entity.QOrders.orders;
import static com.example.java.orders.entity.QOrderItem.orderItem;
import static com.example.java.product.entity.QOptions.options;
import static com.example.java.product.entity.QProduct.product;
import static com.example.java.product.entity.QProductImage.productImage;
import static com.example.java.delivery.entity.QDelivery.delivery;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.java.mypage.dto.MyPageOrderDto;
import com.example.java.product.entity.QProductImage;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MyPageQueryRepositoryImpl implements MyPageQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    @Override
    public List<MyPageOrderDto> findOrdersByMemberSeq(Long memberSeq, String keyword) {
        QProductImage subImage = new QProductImage("subImage");

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(orders.memberSeq.eq(memberSeq));

        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(product.productName.like("%" + keyword.trim() + "%"));
        }

        List<Tuple> results = queryFactory
                .select(
                        orders.orderDate,
                        delivery.status,
                        productImage.imageUrl,
                        orderItem.productName,
                        orderItem.finalPrice,
                        orderItem.quantity,
                        delivery.tracking_number
                )
                .from(orders)
                .join(orderItem).on(orders.seq.eq(orderItem.orderSeq))
                .join(options).on(orderItem.optionsSeq.eq(options.seq))
                .join(product).on(options.product.seq.eq(product.seq))
                .leftJoin(delivery).on(orders.seq.eq(delivery.orders.seq))
                .leftJoin(productImage).on(
                        productImage.productSeq.eq(product.seq)
                                .and(productImage.thumbnailYn.eq("Y"))
                                .and(productImage.status.eq("NORMAL"))
                                .and(productImage.seq.eq(
                                        JPAExpressions
                                                .select(subImage.seq.min())
                                                .from(subImage)
                                                .where(
                                                        subImage.productSeq.eq(product.seq),
                                                        subImage.thumbnailYn.eq("Y"),
                                                        subImage.status.eq("NORMAL")
                                                )
                                ))
                )
                .where(builder)
                .orderBy(orders.seq.desc())
                .fetch();

        return results.stream()
                .map(row -> {
                    String orderDateStr = "";
                    if (row.get(orders.orderDate) != null) {
                        orderDateStr = row.get(orders.orderDate).format(DATE_FORMATTER);
                    }

                    String deliveryStatus = row.get(delivery.status);
                    if (deliveryStatus == null || deliveryStatus.trim().isEmpty()) {
                        deliveryStatus = "배송준비중";
                    }

                    String imageUrl = row.get(productImage.imageUrl);
                    if (imageUrl == null || imageUrl.trim().isEmpty()) {
                        imageUrl = "/images/default-product.png";
                    }

                    String trackingNum = row.get(delivery.tracking_number);
                    if (trackingNum == null || trackingNum.trim().isEmpty()) {
                        trackingNum = "발급대기";
                    }

                    return MyPageOrderDto.builder()
                            .orderDate(orderDateStr)
                            .deliveryStatus(deliveryStatus)
                            .image(imageUrl)
                            .name(row.get(orderItem.productName))
                            .price(row.get(orderItem.finalPrice))
                            .qty(row.get(orderItem.quantity))
                            .trackingNumber(trackingNum)
                            .build();
                })
                .toList();
    }
}
