package com.example.java.orders.service;

import com.example.java.member.entity.DeliveryAddress;
import com.example.java.member.service.MemberAddressService;
import com.example.java.orders.dto.CheckoutItemDto;
import com.example.java.orders.dto.CheckoutRequestDto;
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
    private final MemberAddressService memberAddressService;

    private static final Double DEFAULT_LATITUDE = 37.5665000;
    private static final Double DEFAULT_LONGITUDE = 126.9780000;

    @Transactional
    public OrderCreateResultDto createOrderFromCheckout(CheckoutRequestDto requestDto,
                                                        Long memberSeq) {

        validateCheckoutRequest(requestDto, memberSeq);

        /*
            체크한 장바구니 상품만 조회한다.
         */
        List<CheckoutItemDto> items =
                ordersViewService.getCheckoutItems(memberSeq, requestDto.getCartSeq());

        if (items.isEmpty()) {
            throw new IllegalStateException("선택한 장바구니 상품이 없습니다.");
        }

        /*
            서버에서 핫딜, 쿠폰, 배송비를 다시 계산한다.
            화면에서 넘어온 amount는 신뢰하지 않는다.
         */
        PriceSummaryDto priceSummary =
                ordersViewService.getPriceSummary(
                        memberSeq,
                        requestDto.getMemberCouponSeq(),
                        requestDto.getCartSeq()
                );

        DeliveryInfo deliveryInfo = resolveDeliveryInfo(requestDto, memberSeq);

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

        if (requestDto.getCartSeq() == null || requestDto.getCartSeq().isEmpty()) {
            throw new IllegalArgumentException("결제할 장바구니 상품을 선택해야 합니다.");
        }

        if (requestDto.getAgreeRequired() == null || !requestDto.getAgreeRequired()) {
            throw new IllegalArgumentException("주문 동의가 필요합니다.");
        }

        if (requestDto.getPaymentMethod() == null || requestDto.getPaymentMethod().isBlank()) {
            throw new IllegalArgumentException("결제수단을 선택해야 합니다.");
        }

        if (requestDto.getDeliveryType() == null || requestDto.getDeliveryType().isBlank()) {
            throw new IllegalArgumentException("배송지 선택 방식이 없습니다.");
        }

        if ("EXISTING".equals(requestDto.getDeliveryType())) {
            if (requestDto.getDeliveryAddressSeq() == null) {
                throw new IllegalArgumentException("기존 배송지를 선택해야 합니다.");
            }
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

    private DeliveryInfo resolveDeliveryInfo(CheckoutRequestDto requestDto,
                                             Long memberSeq) {

        if ("NEW".equals(requestDto.getDeliveryType())) {
            return new DeliveryInfo(
                    requestDto.getZipcode(),
                    requestDto.getAddress(),
                    requestDto.getAddressDetail()
            );
        }

        if ("EXISTING".equals(requestDto.getDeliveryType())) {
            DeliveryAddress selectedAddress =
                    memberAddressService.getAddress(requestDto.getDeliveryAddressSeq(), memberSeq);

            return new DeliveryInfo(
                    selectedAddress.getZipcode(),
                    selectedAddress.getAddress(),
                    selectedAddress.getAddressDetail()
            );
        }

        throw new IllegalArgumentException("알 수 없는 배송지 선택 방식입니다. deliveryType=" + requestDto.getDeliveryType());
    }

    private void createOrderItems(Long orderSeq,
                                  List<CheckoutItemDto> items,
                                  int totalCouponDiscount) {

        /*
            쿠폰은 핫딜 적용 후 상품금액 기준으로 배분한다.
         */
        int totalAfterHotdealPrice = items.stream()
                .mapToInt(CheckoutItemDto::totalPrice)
                .sum();

        int remainingCouponDiscount = totalCouponDiscount;

        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            CheckoutItemDto item = items.get(i);

            int lineOriginalTotal = item.originalTotalPrice();
            int lineHotdealDiscount = item.hotdealDiscountTotal();
            int lineAfterHotdealTotal = item.totalPrice();

            int itemCouponDiscount;

            if (i == items.size() - 1) {
                itemCouponDiscount = remainingCouponDiscount;
            } else {
                itemCouponDiscount = totalAfterHotdealPrice == 0
                        ? 0
                        : lineAfterHotdealTotal * totalCouponDiscount / totalAfterHotdealPrice;

                remainingCouponDiscount -= itemCouponDiscount;
            }

            if (itemCouponDiscount > lineAfterHotdealTotal) {
                itemCouponDiscount = lineAfterHotdealTotal;
            }

            int lineFinalTotal = lineAfterHotdealTotal - itemCouponDiscount;

            if (lineFinalTotal < 0) {
                lineFinalTotal = 0;
            }

            int finalUnitPrice = item.quantity() == 0
                    ? 0
                    : lineFinalTotal / item.quantity();

            OrderItem orderItem = OrderItem.builder()
                    .orderSeq(orderSeq)
                    .participationSeq(null)
                    .optionsSeq(item.optionsSeq())
                    .productName(item.name())
                    .quantity(item.quantity())

                    /*
                        상품 1개 원가.
                     */
                    .originalPrice(item.originalPrice())

                    /*
                        이 주문상품 라인의 핫딜 할인 총액.
                     */
                    .hotdealDiscount(lineHotdealDiscount)

                    /*
                        이 주문상품 라인의 쿠폰 할인 총액.
                     */
                    .couponDiscount(itemCouponDiscount)

                    .participationDiscount(0)

                    /*
                        핫딜 + 쿠폰 적용 후 1개당 최종 가격.
                     */
                    .finalPrice(finalUnitPrice)

                    /*
                        핫딜 + 쿠폰 적용 후 라인 최종 금액.
                     */
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