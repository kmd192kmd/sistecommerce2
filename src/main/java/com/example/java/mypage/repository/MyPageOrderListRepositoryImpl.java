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
import com.example.java.mypage.dto.MyPageCancelReturnDto;
import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.Orders;
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
                        orderItem.seq,
                        product.seq,
                        orderItem.productName,
                        product.thumbnailUrl,
                        orderItem.finalPrice,
                        orderItem.quantity,
                        orderItem.itemStatus,
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
            		.orderItemSeq(row.get(orderItem.seq))
                    .productSeq(row.get(product.seq))
                    .name(row.get(orderItem.productName))
                    .image(imageUrl)
                    .price(row.get(orderItem.finalPrice))
                    .qty(row.get(orderItem.quantity))
                    .itemStatus(row.get(orderItem.itemStatus))
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

            boolean allCanceledOrReturned = !entry.getValue().isEmpty() && entry.getValue().stream()
                    .allMatch(item -> item.getItemStatus() != null && (item.getItemStatus() == 6 || item.getItemStatus() == 9));
            String defaultStatus = allCanceledOrReturned ? "CANCELED" : DELIVERY_READY_STATUS;

            deliveryGroups.add(MyPageDeliveryDto.builder()
                    .companyName(entry.getKey())
                    .deliveryStatus(info != null ? info.status() : defaultStatus)
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

    @Override
    public List<MyPageCancelReturnDto> findCancelReturnsByMemberSeq(Long memberSeq) {
        // 1. 취소/반품 상태인 OrderItem & Orders & Product 조회 (itemStatus: 6, 7, 8, 9)
        List<Tuple> rows = queryFactory
                .select(
                        orderItem,
                        orders,
                        product
                )
                .from(orderItem)
                .join(orders).on(orderItem.orderSeq.eq(orders.seq))
                .join(options).on(orderItem.optionsSeq.eq(options.seq))
                .join(product).on(options.product.seq.eq(product.seq))
                .where(orders.memberSeq.eq(memberSeq)
                        .and(orderItem.itemStatus.in(6, 7, 8, 9)))
                .orderBy(orderItem.seq.desc())
                .fetch();

        if (rows.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orderItemSeqs = rows.stream()
                .map(r -> r.get(orderItem).getSeq())
                .toList();

        // 2. Refund 정보 조회
        List<com.example.java.orders.entity.Refund> refunds = queryFactory
                .selectFrom(com.example.java.orders.entity.QRefund.refund)
                .where(com.example.java.orders.entity.QRefund.refund.orderItemSeq.in(orderItemSeqs))
                .fetch();
        Map<Long, com.example.java.orders.entity.Refund> refundMap = new HashMap<>();
        for (com.example.java.orders.entity.Refund ref : refunds) {
            refundMap.put(ref.getOrderItemSeq(), ref);
        }

        // 3. ReturnRequest & Returns 정보 조회
        List<com.example.java.orders.entity.ReturnRequest> returnRequests = queryFactory
                .selectFrom(com.example.java.orders.entity.QReturnRequest.returnRequest)
                .leftJoin(com.example.java.orders.entity.QReturnRequest.returnRequest.refundReason).fetchJoin()
                .where(com.example.java.orders.entity.QReturnRequest.returnRequest.orderItemSeq.in(orderItemSeqs))
                .fetch();

        Map<Long, com.example.java.orders.entity.ReturnRequest> returnReqMap = new HashMap<>();
        for (com.example.java.orders.entity.ReturnRequest req : returnRequests) {
            returnReqMap.put(req.getOrderItemSeq(), req);
        }

        List<Long> returnReqSeqs = returnRequests.stream().map(com.example.java.orders.entity.ReturnRequest::getSeq).toList();
        Map<Long, com.example.java.orders.entity.Returns> returnsMap = new HashMap<>();
        if (!returnReqSeqs.isEmpty()) {
            List<com.example.java.orders.entity.Returns> returnsList = queryFactory
                    .selectFrom(com.example.java.orders.entity.QReturns.returns)
                    .leftJoin(com.example.java.orders.entity.QReturns.returns.deliveryCompany).fetchJoin()
                    .where(com.example.java.orders.entity.QReturns.returns.returnRequest.seq.in(returnReqSeqs))
                    .fetch();
            for (com.example.java.orders.entity.Returns ret : returnsList) {
                if (ret.getReturnRequest() != null) {
                    returnsMap.put(ret.getReturnRequest().getSeq(), ret);
                }
            }
        }

        // 4. Dto 조립
        List<MyPageCancelReturnDto> dtoList = new ArrayList<>();
        for (Tuple row : rows) {
            OrderItem item = row.get(orderItem);
            Orders order = row.get(orders);
            com.example.java.product.entity.Product prod = row.get(product);

            if (item == null || order == null) continue;

            String type = (item.getItemStatus() == 6) ? "CANCEL" : "RETURN";
            String statusText = "";
            if (item.getItemStatus() == 6) statusText = "주문취소";
            else if (item.getItemStatus() == 7) statusText = "반품요청";
            else if (item.getItemStatus() == 8) statusText = "반품진행중";
            else if (item.getItemStatus() == 9) statusText = "반품완료";

            MyPageCancelReturnDto.MyPageCancelReturnDtoBuilder builder = MyPageCancelReturnDto.builder()
                    .orderItemSeq(item.getSeq())
                    .orderSeq(order.getSeq())
                    .orderUid(order.getOrderUid())
                    .orderDate(order.getOrderDate() != null ? order.getOrderDate().format(DATE_FORMATTER) : "")
                    .productName(item.getProductName())
                    .productPrice(item.getFinalPrice())
                    .quantity(item.getQuantity())
                    .itemStatus(item.getItemStatus())
                    .thumbnailUrl(prod != null && prod.getThumbnailUrl() != null ? prod.getThumbnailUrl() : "/images/default-product.png")
                    .type(type)
                    .statusText(statusText);

            if ("CANCEL".equals(type)) {
                com.example.java.orders.entity.Refund ref = refundMap.get(item.getSeq());
                if (ref != null) {
                    builder.requestDate(ref.getRequestDate() != null ? ref.getRequestDate().format(DATE_FORMATTER) : "")
                            .uid(ref.getRefundUid())
                            .completedDate(ref.getCompleteDate() != null ? ref.getCompleteDate().format(DATE_FORMATTER) : "")
                            .reason("고객 주문 취소")
                            .refundPrice(ref.getRefundPrice())
                            .paymentMethod(order.getPaymentStatus() == 5 || order.getPaymentStatus() == 6 ? "토스 페이먼츠" : "카드")
                            .originalPrice(ref.getRefundProductPrice())
                            .discountPrice(ref.getRefundCoupon() + ref.getRefundHotdeal())
                            .deliveryFee(0);
                }
            } else {
                com.example.java.orders.entity.ReturnRequest req = returnReqMap.get(item.getSeq());
                if (req != null) {
                    com.example.java.orders.entity.Returns ret = returnsMap.get(req.getSeq());
                    com.example.java.orders.entity.Refund ref = refundMap.get(item.getSeq());
                    
                    builder.requestDate(req.getRequestDate() != null ? req.getRequestDate().format(DATE_FORMATTER) : "")
                            .uid(req.getReturnUid())
                            .completedDate(req.getCompletedDate() != null ? req.getCompletedDate().format(DATE_FORMATTER) : "")
                            .reason(req.getRefundReason() != null ? req.getRefundReason().getReason() : "고객 반품 신청");

                    if (ref != null) {
                        builder.refundPrice(ref.getRefundPrice())
                                .paymentMethod("토스 페이먼츠")
                                .originalPrice(ref.getRefundProductPrice())
                                .discountPrice(ref.getRefundCoupon() + ref.getRefundHotdeal())
                                .deliveryFee(ret != null && ret.getDeliveryCompany() != null ? 3000 : 0);
                    } else {
                        builder.refundPrice(item.getSubTotalPrice())
                                .paymentMethod("토스 페이먼츠")
                                .originalPrice(item.getOriginalPrice() * item.getQuantity())
                                .discountPrice((item.getOriginalPrice() * item.getQuantity()) - item.getSubTotalPrice())
                                .deliveryFee(3000);
                    }
                }
            }

            dtoList.add(builder.build());
        }

        return dtoList;
    }
}
