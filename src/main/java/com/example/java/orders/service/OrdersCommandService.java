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

    /**
     * 좌표 입력 기능 연동 전 임시 좌표.
     *
     * TODO 실제 연동 시 교체
     * - 주소 검색 API 또는 배송지 테이블의 좌표값을 사용해야 한다.
     */
    private static final Double DEFAULT_LATITUDE = 37.5665000;
    private static final Double DEFAULT_LONGITUDE = 126.9780000;

    /**
     * 결제하기 버튼 클릭 시 주문 데이터를 생성한다.
     *
     * 수정된 DDL 기준:
     * - orders 먼저 INSERT
     * - 생성된 orders.seq를 order_item.order_seq에 넣어서 order_item 여러 개 INSERT
     *
     * 로그인 연동 후:
     * - memberSeq는 Controller에서 현재 로그인한 회원 기준으로 전달받는다.
     */
    @Transactional
    public OrderCreateResultDto createOrderFromCheckout(CheckoutRequestDto requestDto,
                                                        Long memberSeq) {

        validateCheckoutRequest(requestDto, memberSeq);

        List<CheckoutItemDto> items = ordersViewService.getCheckoutItems();

        if (items.isEmpty()) {
            throw new IllegalStateException("주문 상품이 없습니다.");
        }

        /*
            화면에서 넘어온 amount는 신뢰하지 않는다.
            서버에서 상품/쿠폰/배송비 기준으로 다시 계산한다.
         */
        PriceSummaryDto priceSummary = ordersViewService.getPriceSummary(requestDto.getCouponSeq());

        DeliveryInfo deliveryInfo = resolveDeliveryInfo(requestDto);

        int productTotalPrice = priceSummary.productTotalPrice();
        int totalCouponDiscount = priceSummary.couponDiscount();
        int hotdealDiscount = priceSummary.hotdealDiscount();
        int finalPrice = priceSummary.finalPrice();

        String orderUid = createOrderUid();

        /*
            1. orders 먼저 저장한다.
            변경된 구조에서는 orders가 order_item을 직접 참조하지 않는다.
         */
        Orders order = Orders.builder()
                .memberSeq(memberSeq)

                /*
                    현재 테스트 단계에서는 coupon.seq를 직접 받고 있으므로 member_coupon_seq는 null.
                    TODO 실제 연동 시 교체
                    - 실제 회원 쿠폰 기능을 붙이면 member_coupon.seq를 받아서 저장한다.
                 */
                .memberCouponSeq(null)

                .orderUid(orderUid)
                .productTotalPrice(productTotalPrice)
                .couponDiscount(totalCouponDiscount)
                .hotdealDiscount(hotdealDiscount)
                .finalPrice(finalPrice)
                .totalRefundPrice(0)
                .remainPrice(finalPrice)

                /*
                    DDL 주석 기준:
                    order_status = 1: 결제대기
                    payment_status = 0: 결제대기
                 */
                .orderStatus(1)
                .paymentStatus(0)

                .orderDate(LocalDateTime.now())
                .regdate(LocalDateTime.now())

                .zipcode(deliveryInfo.zipcode())
                .address(deliveryInfo.address())
                .addressDetail(deliveryInfo.addressDetail())

                .currLatitude(DEFAULT_LATITUDE)
                .currLongitude(DEFAULT_LONGITUDE)
                .build();

        Orders savedOrder = ordersRepository.save(order);

        /*
            2. 저장된 orders.seq를 order_item.order_seq에 넣어서 주문상품들을 저장한다.
         */
        createOrderItems(savedOrder.getSeq(), items, totalCouponDiscount);

        return new OrderCreateResultDto(
                savedOrder.getSeq(),
                savedOrder.getOrderUid(),
                savedOrder.getFinalPrice()
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

        /*
            기존 배송지 선택은 아직 DB 연동 전이므로 화면 테스트용 배송지 목록에서 찾는다.

            TODO 실제 연동 시 교체
            - delivery_address 테이블에서 로그인 회원의 deliveryAddressSeq를 조회
            - 조회한 주소를 orders에 저장
         */
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

            /*
                쿠폰 할인금액을 상품금액 비율대로 각 order_item에 배분한다.
                마지막 상품에는 남은 할인금액을 넣어서 절삭 오차를 방지한다.
             */
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