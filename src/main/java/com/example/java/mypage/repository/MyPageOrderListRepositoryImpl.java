package com.example.java.mypage.repository;

import static com.example.java.delivery.entity.QDelivery.delivery;
import static com.example.java.orders.entity.QOrderItem.orderItem;
import static com.example.java.orders.entity.QOrders.orders;
import static com.example.java.product.entity.QOptions.options;
import static com.example.java.product.entity.QProduct.product;
import static com.example.java.product.entity.QSeller.seller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.example.java.mypage.dto.MyPageDeliveryDto;
import com.example.java.mypage.dto.MyPageOrderItemDto;
import com.example.java.mypage.dto.MyPageOrderListDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MyPageOrderListRepositoryImpl implements MyPageOrderListRepository {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final String DELIVERY_READY_COMPANY = "배송 준비중";
    private static final String TRACKING_PENDING = "발급대기";
    private static final String DELIVERY_READY_STATUS = "READY";

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MyPageOrderListDto> findOrdersByMemberSeq(Long memberSeq, String keyword, String period) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(orders.memberSeq.eq(memberSeq));

        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(product.productName.like("%" + keyword.trim() + "%"));
        }

        if (period == null || period.trim().isEmpty() || "6months".equalsIgnoreCase(period)) {
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            builder.and(orders.orderDate.goe(sixMonthsAgo));
        } else {
            try {
                int year = Integer.parseInt(period);
                LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
                LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);
                builder.and(orders.orderDate.between(startOfYear, endOfYear));
            } catch (NumberFormatException e) {
                LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
                builder.and(orders.orderDate.goe(sixMonthsAgo));
            }
        }

        List<Long> matchedOrderSeqs = queryFactory
                .select(orders.seq)
                .distinct()
                .from(orders)
                .join(orderItem).on(orders.seq.eq(orderItem.orderSeq))
                .join(options).on(orderItem.optionsSeq.eq(options.seq))
                .join(product).on(options.product.seq.eq(product.seq))
                .where(builder)
                .orderBy(orders.seq.desc())
                .fetch();

        if (matchedOrderSeqs.isEmpty()) {
            return new ArrayList<>();
        }

        List<Tuple> itemRows = queryFactory
                .select(
                        orderItem.orderSeq,
                        product.seq,
                        orderItem.productName,
                        product.thumbnailUrl,
                        orderItem.finalPrice,
                        orderItem.quantity,
                        seller.deliveryCompany.name
                )
                .from(orderItem)
                .join(options).on(orderItem.optionsSeq.eq(options.seq))
                .join(product).on(options.product.seq.eq(product.seq))
                .join(seller).on(product.sellerSeq.eq(seller.seq))
                .where(orderItem.orderSeq.in(matchedOrderSeqs))
                .fetch();

        List<Tuple> deliveryRows = queryFactory
                .select(
                        delivery.orders.seq,
                        delivery.deliveryCompany.name,
                        delivery.status,
                        delivery.tracking_number,
                        delivery.completed_at
                )
                .from(delivery)
                .where(delivery.orders.seq.in(matchedOrderSeqs))
                .fetch();

        Map<Long, Map<String, List<MyPageOrderItemDto>>> itemsByOrderAndCompany = groupItemsByOrderAndCompany(itemRows);
        Map<Long, Map<String, DeliveryInfo>> deliveryInfoByOrderAndCompany = groupDeliveryInfoByOrderAndCompany(deliveryRows);
        Map<Long, String> dateByOrderSeq = findOrderDates(matchedOrderSeqs);

        List<MyPageOrderListDto> finalOrders = new ArrayList<>();
        for (Long orderSeq : matchedOrderSeqs) {
            List<MyPageDeliveryDto> deliveryGroups = buildDeliveryCompanyGroups(
                    itemsByOrderAndCompany.get(orderSeq),
                    deliveryInfoByOrderAndCompany.get(orderSeq)
            );

            boolean allDelivered = !deliveryGroups.isEmpty() && deliveryGroups.stream()
                    .allMatch(group -> "DELIVERED".equals(group.getDeliveryStatus()));

            finalOrders.add(MyPageOrderListDto.builder()
                    .orderSeq(orderSeq)
                    .orderDate(dateByOrderSeq.get(orderSeq))
                    .deliveries(deliveryGroups)
                    .allDelivered(allDelivered)
                    .build());
        }

        return finalOrders;
    }

    private Map<Long, Map<String, List<MyPageOrderItemDto>>> groupItemsByOrderAndCompany(List<Tuple> itemRows) {
        Map<Long, Map<String, List<MyPageOrderItemDto>>> groupedItems = new LinkedHashMap<>();

        for (Tuple row : itemRows) {
            Long orderSeq = row.get(orderItem.orderSeq);
            String companyName = valueOrDefault(row.get(seller.deliveryCompany.name), DELIVERY_READY_COMPANY);
            String imageUrl = valueOrDefault(row.get(product.thumbnailUrl), "/images/default-product.png");

            MyPageOrderItemDto itemDto = MyPageOrderItemDto.builder()
                    .productSeq(row.get(product.seq))
                    .name(row.get(orderItem.productName))
                    .image(imageUrl)
                    .price(row.get(orderItem.finalPrice))
                    .qty(row.get(orderItem.quantity))
                    .build();

            groupedItems
                    .computeIfAbsent(orderSeq, key -> new LinkedHashMap<>())
                    .computeIfAbsent(companyName, key -> new ArrayList<>())
                    .add(itemDto);
        }

        return groupedItems;
    }

    private Map<Long, Map<String, DeliveryInfo>> groupDeliveryInfoByOrderAndCompany(List<Tuple> deliveryRows) {
        Map<Long, Map<String, DeliveryInfo>> groupedDeliveryInfo = new HashMap<>();

        for (Tuple row : deliveryRows) {
            Long orderSeq = row.get(delivery.orders.seq);
            String companyName = valueOrDefault(row.get(delivery.deliveryCompany.name), DELIVERY_READY_COMPANY);

            groupedDeliveryInfo
                    .computeIfAbsent(orderSeq, key -> new HashMap<>())
                    .put(companyName, new DeliveryInfo(
                            valueOrDefault(row.get(delivery.status), DELIVERY_READY_STATUS),
                            valueOrDefault(row.get(delivery.tracking_number), TRACKING_PENDING),
                            formatDate(row.get(delivery.completed_at))
                    ));
        }

        return groupedDeliveryInfo;
    }

    private Map<Long, String> findOrderDates(List<Long> orderSeqs) {
        List<Tuple> orderDates = queryFactory
                .select(orders.seq, orders.orderDate)
                .from(orders)
                .where(orders.seq.in(orderSeqs))
                .fetch();

        Map<Long, String> dateByOrderSeq = new HashMap<>();
        for (Tuple row : orderDates) {
            String date = "";
            if (row.get(orders.orderDate) != null) {
                date = row.get(orders.orderDate).format(DATE_FORMATTER);
            }
            dateByOrderSeq.put(row.get(orders.seq), date);
        }
        return dateByOrderSeq;
    }

    private List<MyPageDeliveryDto> buildDeliveryCompanyGroups(
            Map<String, List<MyPageOrderItemDto>> companyItems,
            Map<String, DeliveryInfo> deliveryInfoByCompany
    ) {
        List<MyPageDeliveryDto> deliveryGroups = new ArrayList<>();

        if (companyItems == null || companyItems.isEmpty()) {
            deliveryGroups.add(MyPageDeliveryDto.builder()
                    .companyName(DELIVERY_READY_COMPANY)
                    .deliveryStatus(DELIVERY_READY_STATUS)
                    .trackingNumber(TRACKING_PENDING)
                    .completedAt("")
                    .items(new ArrayList<>())
                    .build());
            return deliveryGroups;
        }

        Map<String, DeliveryInfo> safeDeliveryInfo =
                deliveryInfoByCompany != null ? deliveryInfoByCompany : new HashMap<>();

        for (Map.Entry<String, List<MyPageOrderItemDto>> entry : companyItems.entrySet()) {
            DeliveryInfo info = safeDeliveryInfo.get(entry.getKey());

            deliveryGroups.add(MyPageDeliveryDto.builder()
                    .companyName(entry.getKey())
                    .deliveryStatus(info != null ? info.status() : DELIVERY_READY_STATUS)
                    .trackingNumber(info != null ? info.trackingNumber() : TRACKING_PENDING)
                    .completedAt(info != null ? info.completedAt() : "")
                    .items(entry.getValue())
                    .build());
        }

        return deliveryGroups;
    }

    private String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_FORMATTER);
    }

    private record DeliveryInfo(String status, String trackingNumber, String completedAt) {
    }
}
