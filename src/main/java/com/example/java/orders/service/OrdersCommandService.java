package com.example.java.orders.service;

import com.example.java.orders.dto.CheckoutItemDto;
import com.example.java.orders.dto.CheckoutRequestDto;
import com.example.java.orders.dto.DeliveryAddressDto;
import com.example.java.orders.dto.OrderCreateResultDto;
import com.example.java.orders.dto.PriceSummaryDto;
import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.Orders;
import com.example.java.orders.repository.OrderItemRepository;
import com.example.java.orders.repository.OrdersRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdersCommandService {

    private final OrdersViewService ordersViewService;
    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;

    private static final Double DEFAULT_LATITUDE = 37.5665000;
    private static final Double DEFAULT_LONGITUDE = 126.9780000;

    @Transactional
    public OrderCreateResultDto createOrderFromCheckout(CheckoutRequestDto requestDto,
                                                        Long memberSeq) {

        validateCheckoutRequest(requestDto, memberSeq);

        /*
            하드코딩 옵션이 아니라 로그인 회원의 장바구니 상품을 조회한다.
         */
        List<CheckoutItemDto> items = ordersViewService.getCheckoutItems(memberSeq);

        if (items.isEmpty()) {
            throw new IllegalStateException("장바구니에 결제할 상품이 없습니다.");
        }

        /*
            화면에서 넘어온 amount는 신뢰하지 않는다.
            장바구니 상품 + 회원 쿠폰 기준으로 서버에서 다시 계산한다.
         */
        PriceSummaryDto priceSummary =
                ordersViewService.getPriceSummary(memberSeq, requestDto.getMemberCouponSeq());

        DeliveryInfo deliveryInfo = resolveDeliveryInfo(requestDto);

        int productTotalPrice = priceSummary.productTotalPrice();
        int totalCouponDiscount = priceSummary.couponDiscount();
        int hotdealDiscount = priceSummary.hotdealDiscount();
        int finalPrice = priceSummary.finalPrice();

        String orderUid = createOrderUid();
        String orderName = createOrderName(items);

        Orders order = Orders.builder()
                .memberSeq(memberSeq)
                .memberCouponSeq(requestDto.getMemberCouponSeq())
                .orderUid(orderUid)
                .productTotalPrice(productTotalPrice)
                .couponDiscount(totalCouponDiscount)
                .hotdealDiscount(hotdealDiscount)
                .finalPrice(finalPrice)
                .totalRefundPrice(0)
                .remainPrice(finalPrice)
                .orderStatus(1)
                .paymentStatus(0)
                .orderDate(null)
                .regdate(LocalDateTime.now())
                .zipcode(deliveryInfo.zipcode())
                .address(deliveryInfo.address())
                .addressDetail(deliveryInfo.addressDetail())
                .currLatitude(DEFAULT_LATITUDE)
                .currLongitude(DEFAULT_LONGITUDE)
                .build();

        Orders savedOrder = ordersRepository.save(order);

        createOrderItems(savedOrder.getSeq(), items, totalCouponDiscount);

        return new OrderCreateResultDto(
                savedOrder.getSeq(),
                savedOrder.getOrderUid(),
                savedOrder.getFinalPrice(),
                orderName
        );
    }

    private void validateCheckoutRequest(CheckoutRequestDto requestDto,
                                         Long memberSeq) {

        if (memberSeq == null) {
            throw new IllegalArgumentException("로그인 회원 번호가 없습니다.");
        }

        if (requestDto.getAgreeRequired() == null || !requestDto.getAgreeRequired()) {
            throw new IllegalArgumentException("주문 동의가 필요합니다.");
        }

        if (requestDto.getPaymentMethod() == null || requestDto.getPaymentMethod().isBlank()) {
            throw new IllegalArgumentException("결제수단을 선택해야 합니다.");
        }

        if ("NEW".equals(requestDto.getDeliveryType())) {
            if (isBlank(requestDto.getRecipientName())) {
                throw new IllegalArgumentException("받는 사람을 입력해야 합니다.");
            }

            if (isBlank(requestDto.getRecipientPhone())) {
                throw new IllegalArgumentException("연락처를 입력해야 합니다.");
            }

            if (isBlank(requestDto.getZipcode())) {
                throw new IllegalArgumentException("우편번호를 입력해야 합니다.");
            }

            if (isBlank(requestDto.getAddress())) {
                throw new IllegalArgumentException("주소를 입력해야 합니다.");
            }
        }
    }

    private DeliveryInfo resolveDeliveryInfo(CheckoutRequestDto requestDto) {
        if ("NEW".equals(requestDto.getDeliveryType())) {
            return new DeliveryInfo(
                    requestDto.getZipcode(),
                    requestDto.getAddress(),
                    requestDto.getAddressDetail()
            );
        }

        List<DeliveryAddressDto> addresses = ordersViewService.getDeliveryAddresses();

        DeliveryAddressDto selected = addresses.stream()
                .filter(addr -> addr.seq().equals(requestDto.getDeliveryAddressSeq()))
                .findFirst()
                .orElse(addresses.get(0));

        return new DeliveryInfo(
                selected.zipcode(),
                selected.address(),
                selected.addressDetail()
        );
    }

    private void createOrderItems(Long orderSeq,
                                  List<CheckoutItemDto> items,
                                  int totalCouponDiscount) {

        int totalProductPrice = items.stream()
                .mapToInt(CheckoutItemDto::totalPrice)
                .sum();

        int remainingCouponDiscount = totalCouponDiscount;

        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            CheckoutItemDto item = items.get(i);

            int lineOriginalTotal = item.totalPrice();

            int itemCouponDiscount;

            if (i == items.size() - 1) {
                itemCouponDiscount = remainingCouponDiscount;
            } else {
                itemCouponDiscount = totalProductPrice == 0
                        ? 0
                        : lineOriginalTotal * totalCouponDiscount / totalProductPrice;

                remainingCouponDiscount -= itemCouponDiscount;
            }

            if (itemCouponDiscount > lineOriginalTotal) {
                itemCouponDiscount = lineOriginalTotal;
            }

            int lineFinalTotal = lineOriginalTotal - itemCouponDiscount;

            int finalUnitPrice = item.quantity() == 0
                    ? 0
                    : lineFinalTotal / item.quantity();

            OrderItem orderItem = OrderItem.builder()
                    .orderSeq(orderSeq)
                    .participationSeq(null)
                    .optionsSeq(item.optionsSeq())
                    .productName(item.name())
                    .quantity(item.quantity())
                    .originalPrice(item.price())
                    .hotdealDiscount(0)
                    .couponDiscount(itemCouponDiscount)
                    .participationDiscount(0)
                    .finalPrice(finalUnitPrice)
                    .subTotalPrice(lineFinalTotal)
                    .refundQuantity(0)
                    .refundPrice(0)
                    .itemStatus(0)
                    .returnQuantity(null)
                    .build();

            orderItems.add(orderItem);
        }

        orderItemRepository.saveAll(orderItems);
    }

    private String createOrderUid() {
        return "GM-" + System.currentTimeMillis();
    }

    private String createOrderName(List<CheckoutItemDto> items) {
        if (items.isEmpty()) {
            return "주문상품";
        }

        if (items.size() == 1) {
            return items.get(0).name();
        }

        return items.get(0).name() + " 외 " + (items.size() - 1) + "건";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record DeliveryInfo(
            String zipcode,
            String address,
            String addressDetail
    ) {
    }
}