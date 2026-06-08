package com.example.java.mypage.repository;

import static com.example.java.orders.entity.QOrders.orders;
import static com.example.java.orders.entity.QOrderItem.orderItem;
import static com.example.java.product.entity.QOptions.options;
import static com.example.java.product.entity.QProduct.product;
import static com.example.java.product.entity.QSeller.seller;
import static com.example.java.delivery.entity.QDelivery.delivery;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.springframework.stereotype.Repository;

import com.example.java.mypage.dto.MyPageOrderListDto;
import com.example.java.mypage.dto.MyPageOrderItemDto;
import com.example.java.mypage.dto.MyPageDeliveryDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MyPageOrderListRepositoryImpl implements MyPageOrderListRepository {

    private final JPAQueryFactory queryFactory;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    @Override
    public List<MyPageOrderListDto> findOrdersByMemberSeq(Long memberSeq, String keyword) {

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(orders.memberSeq.eq(memberSeq));

        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(product.productName.like("%" + keyword.trim() + "%"));
        }

        // 1. Fetch matching Order seqs
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

        // 2. Fetch all Order Items for matched orders with their seller's delivery company name
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

        // 3. Fetch all Deliveries for matched orders
        List<Tuple> deliveryRows = queryFactory
                .select(
                        delivery.orders.seq,
                        delivery.deliveryCompany.name,
                        delivery.status,
                        delivery.tracking_number
                )
                .from(delivery)
                .where(delivery.orders.seq.in(matchedOrderSeqs))
                .fetch();

        // Group items by orderSeq and delivery company name
        Map<Long, Map<String, List<MyPageOrderItemDto>>> itemsByOrderAndCompany = new HashMap<>();
        for (Tuple row : itemRows) {
            Long orderSeq = row.get(orderItem.orderSeq);
            String imageUrl = row.get(product.thumbnailUrl);
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = "/images/default-product.png";
            }
            String companyName = row.get(seller.deliveryCompany.name);
            if (companyName == null || companyName.trim().isEmpty()) {
                companyName = "배송 준비중";
            }

            MyPageOrderItemDto itemDto = MyPageOrderItemDto.builder()
                    .productSeq(row.get(product.seq))
                    .name(row.get(orderItem.productName))
                    .image(imageUrl)
                    .price(row.get(orderItem.finalPrice))
                    .qty(row.get(orderItem.quantity))
                    .build();

            itemsByOrderAndCompany
                    .computeIfAbsent(orderSeq, k -> new HashMap<>())
                    .computeIfAbsent(companyName, k -> new ArrayList<>())
                    .add(itemDto);
        }

        // Group deliveries by orderSeq and link corresponding items
        Map<Long, List<MyPageDeliveryDto>> deliveriesByOrderSeq = new HashMap<>();
        for (Tuple row : deliveryRows) {
            Long orderSeq = row.get(delivery.orders.seq);
            String deliveryStatus = row.get(delivery.status);
            if (deliveryStatus == null || deliveryStatus.trim().isEmpty()) {
                deliveryStatus = "배송준비중";
            }
            String trackingNum = row.get(delivery.tracking_number);
            if (trackingNum == null || trackingNum.trim().isEmpty()) {
                trackingNum = "발급대기";
            }
            String companyName = row.get(delivery.deliveryCompany.name);
            if (companyName == null || companyName.trim().isEmpty()) {
                companyName = "배송 준비중";
            }

            // Get items shipped by this delivery company under this order
            List<MyPageOrderItemDto> deliveryItems = new ArrayList<>();
            Map<String, List<MyPageOrderItemDto>> companyMap = itemsByOrderAndCompany.get(orderSeq);
            if (companyMap != null) {
                deliveryItems = companyMap.getOrDefault(companyName, new ArrayList<>());
            }

            MyPageDeliveryDto deliveryDto = MyPageDeliveryDto.builder()
                    .companyName(companyName)
                    .deliveryStatus(deliveryStatus)
                    .trackingNumber(trackingNum)
                    .items(deliveryItems)
                    .build();

            deliveriesByOrderSeq.computeIfAbsent(orderSeq, k -> new ArrayList<>()).add(deliveryDto);
        }

        // Load Order Dates
        List<Tuple> orderDates = queryFactory
                .select(orders.seq, orders.orderDate)
                .from(orders)
                .where(orders.seq.in(matchedOrderSeqs))
                .fetch();
        Map<Long, String> dateByOrderSeq = new HashMap<>();
        for (Tuple row : orderDates) {
            Long seq = row.get(orders.seq);
            String dateStr = "";
            if (row.get(orders.orderDate) != null) {
                dateStr = row.get(orders.orderDate).format(DATE_FORMATTER);
            }
            dateByOrderSeq.put(seq, dateStr);
        }

        // Construct final hierarchical DTOs
        List<MyPageOrderListDto> finalOrders = new ArrayList<>();
        for (Long orderSeq : matchedOrderSeqs) {
            List<MyPageDeliveryDto> deliveriesList = deliveriesByOrderSeq.getOrDefault(orderSeq, new ArrayList<>());

            // If no delivery record is present, fallback to a default "배송 준비중" card with all items
            // If no delivery record is present, create separate "배송 준비중" cards for each delivery company
            if (deliveriesList.isEmpty()) {
                Map<String, List<MyPageOrderItemDto>> companyMap = itemsByOrderAndCompany.get(orderSeq);
                if (companyMap != null && !companyMap.isEmpty()) {
                    for (Map.Entry<String, List<MyPageOrderItemDto>> entry : companyMap.entrySet()) {
                        String companyName = entry.getKey();
                        List<MyPageOrderItemDto> companyItems = entry.getValue();

                        deliveriesList.add(MyPageDeliveryDto.builder()
                                .companyName(companyName)
                                .deliveryStatus("READY")
                                .trackingNumber("발급대기")
                                .items(companyItems)
                                .build());
                    }
                } else {
                    deliveriesList.add(MyPageDeliveryDto.builder()
                            .companyName("배송 준비중")
                            .deliveryStatus("READY")
                            .trackingNumber("발급대기")
                            .items(new ArrayList<>())
                            .build());
                }
            }

            boolean allDeliveredVal = !deliveriesList.isEmpty() && deliveriesList.stream()
                    .allMatch(d -> "DELIVERED".equals(d.getDeliveryStatus()));

            finalOrders.add(MyPageOrderListDto.builder()
                    .orderSeq(orderSeq)
                    .orderDate(dateByOrderSeq.get(orderSeq))
                    .deliveries(deliveriesList)
                    .allDelivered(allDeliveredVal)
                    .build());
        }

        return finalOrders;
    }
}
