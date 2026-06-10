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
     * 현재 구조:
     * - 로그인 회원의 장바구니 상품을 기준으로 주문 생성
     * - 로그인 회원이 발급받은 member_coupon 기준으로 쿠폰 적용
     * - 기존 배송지 선택 시 delivery_address 테이블에서 다시 조회 후 orders에 주소 저장
     * - orders 먼저 INSERT
     * - 생성된 orders.seq를 order_item.order_seq에 넣어서 order_item 여러 개 INSERT
     */
    @Transactional
    public OrderCreateResultDto createOrderFromCheckout(CheckoutRequestDto requestDto,
                                                        Long memberSeq) {

        validateCheckoutRequest(requestDto, memberSeq);

        /*
            하드코딩 상품이 아니라 로그인 회원의 장바구니 상품을 조회한다.
         */
        List<CheckoutItemDto> items = ordersViewService.getCheckoutItems(memberSeq);

        if (items.isEmpty()) {
            throw new IllegalStateException("장바구니에 결제할 상품이 없습니다.");
        }

        /*
            화면에서 넘어온 amount는 신뢰하지 않는다.
            서버에서 장바구니 상품 + 회원 쿠폰 기준으로 다시 계산한다.
         */
        PriceSummaryDto priceSummary =
                ordersViewService.getPriceSummary(memberSeq, requestDto.getMemberCouponSeq());

        /*
            배송지 정보는 서버에서 다시 조회한다.
            기존 배송지 선택 시 deliveryAddressSeq가 로그인 회원의 배송지인지 검증한다.
         */
        DeliveryInfo deliveryInfo = resolveDeliveryInfo(requestDto, memberSeq);

        int productTotalPrice = priceSummary.productTotalPrice();
        int totalCouponDiscount = priceSummary.couponDiscount();
        int hotdealDiscount = priceSummary.hotdealDiscount();
        int finalPrice = priceSummary.finalPrice();

        String orderUid = createOrderUid();
        String orderName = createOrderName(items);

        /*
            1. orders 먼저 저장한다.
         */
        Orders order = Orders.builder()
                .memberSeq(memberSeq)

                /*
                    coupon.seq가 아니라 member_coupon.seq를 저장한다.
                 */
                .memberCouponSeq(requestDto.getMemberCouponSeq())

                .orderUid(orderUid)
                .productTotalPrice(productTotalPrice)
                .couponDiscount(totalCouponDiscount)
                .hotdealDiscount(hotdealDiscount)
                .finalPrice(finalPrice)
                .totalRefundPrice(0)
                .remainPrice(finalPrice)

                /*
                    order_status = 1: 결제대기
                    payment_status = 0: 결제대기
                 */
                .orderStatus(1)
                .paymentStatus(0)

                /*
                    order_date는 결제 완료 시점에 PaymentService에서 업데이트한다.
                 */
                .orderDate(null)
                .regdate(LocalDateTime.now())

                /*
                    선택한 배송지 정보를 orders에 저장한다.
                    현재 orders 테이블에는 수령인/연락처 컬럼이 없으므로
                    zipcode, address, address_detail만 저장한다.
                 */
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

        /*
            새 배송지 직접 입력.
            현재는 입력한 주소를 orders에만 저장한다.
            새 배송지도 회원 배송지 테이블에 저장하려면 여기에서 MemberAddressService 저장 메서드를 추가 호출하면 된다.
         */
        if ("NEW".equals(requestDto.getDeliveryType())) {
            return new DeliveryInfo(
                    requestDto.getZipcode(),
                    requestDto.getAddress(),
                    requestDto.getAddressDetail()
            );
        }

        /*
            기존 배송지 선택.
            memberAddressService.getAddress(addressSeq, memberSeq)는
            해당 배송지가 로그인 회원의 배송지인지 검증해야 한다.
         */
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