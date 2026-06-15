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
import com.example.java.product.entity.Options;
import com.example.java.product.repository.OptionsRepository;

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
    private final OptionsRepository optionsRepository;

    private static final Double DEFAULT_LATITUDE = 37.5665000;
    private static final Double DEFAULT_LONGITUDE = 126.9780000;

    @Transactional
    public OrderCreateResultDto createOrderFromCheckout(CheckoutRequestDto requestDto,
                                                        Long memberSeq) {

        validateCheckoutRequest(requestDto, memberSeq);

        /*
            체크한 장바구니 상품만 조회한다.

            바로구매일 경우에는 장바구니를 조회하지 않고,
            상품 상세 화면에서 넘어온 optionsSeq와 quantity 기준으로
            결제 상품 정보를 조회한다.
         */
        List<CheckoutItemDto> items = getCheckoutItemsByRequest(requestDto, memberSeq);

        if (items.isEmpty()) {
            throw new IllegalStateException("선택한 상품이 없습니다.");
        }

        /*
            서버에서 핫딜, 쿠폰, 배송비를 다시 계산한다.
            화면에서 넘어온 amount는 신뢰하지 않는다.

            장바구니 결제와 바로구매 결제 모두
            위에서 만든 items 기준으로 금액을 다시 계산한다.
         */
        PriceSummaryDto priceSummary =
                ordersViewService.getPriceSummaryByItems(
                        memberSeq,
                        requestDto.getMemberCouponSeq(),
                        items
                );

        DeliveryInfo deliveryInfo = resolveDeliveryInfo(requestDto, memberSeq);

        int productTotalPrice = priceSummary.productTotalPrice();
        int totalCouponDiscount = priceSummary.couponDiscount();
        int hotdealDiscount = priceSummary.hotdealDiscount();
        int finalPrice = priceSummary.finalPrice();

        String orderUid = createOrderUid();
        String orderName = createOrderName(items);

        String recipientName = requestDto.getRecipientName() != null ? requestDto.getRecipientName().trim() : "";
        String recipientPhone = requestDto.getRecipientPhone() != null ? requestDto.getRecipientPhone().trim() : "";
        String requestMemo = requestDto.getRequestMemo() != null ? requestDto.getRequestMemo().trim() : "";
        String orderSource = Boolean.TRUE.equals(requestDto.getDirectBuyYn()) ? "DIRECT" : "CART";

        String fieldData = orderSource + "|" + recipientName + "|" + recipientPhone + "|" + requestMemo;
        if (fieldData.length() > 255) {
            fieldData = fieldData.substring(0, 255);
        }

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
                .field(fieldData)
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

    /*
        결제 요청 방식에 따라 주문 상품 목록을 조회한다.

        장바구니 결제:
        - 기존 cartSeq 목록 기준으로 장바구니 상품을 조회한다.

        바로구매:
        - cartSeq를 사용하지 않는다.
        - 상품 상세 화면에서 넘어온 optionsSeq와 quantity 기준으로
          결제 상품 1개를 조회해서 List 형태로 반환한다.
    */
    private List<CheckoutItemDto> getCheckoutItemsByRequest(CheckoutRequestDto requestDto,
                                                            Long memberSeq) {

        if (Boolean.TRUE.equals(requestDto.getDirectBuyYn())) {
            return List.of(
                    ordersViewService.getDirectCheckoutItem(
                            requestDto.getOptionsSeq(),
                            requestDto.getQuantity()
                    )
            );
        }

        return ordersViewService.getCheckoutItems(memberSeq, requestDto.getCartSeq());
    }

    /**
     * 공구 참여용 단건 '결제대기' 주문 생성. (장바구니 기반 createOrderFromCheckout과 별개 —
     * 공구는 1인1상품·수량1·쿠폰/핫딜 없음.)
     *
     * 결제 전 단계라 orderStatus=1(결제대기)/paymentStatus=0(미결제)으로 두고,
     * order_item에 participation_seq를 채워 공구 주문임을 표시한다(이후 결제·환불·재고처리가 이걸로 분기).
     * 반환한 orderUid로 프론트가 토스 결제창을 띄우고, 결제 성공 시 confirmPayment가 마무리한다.
     * 가격은 모두 구매 시점 스냅샷(호출측 공구 서비스가 계산해 넘긴다). 배송지는 회원 기본배송지.
     */
    @Transactional
    public OrderCreateResultDto createGroupBuyOrder(Long memberSeq,
                                                    Long participationSeq,
                                                    Long optionsSeq,
                                                    int originalPrice,
                                                    int finalPrice,
                                                    int participationDiscount) {

        // 같은 참여에 대한 이전 미결제 결제대기 주문을 먼저 정리한다(중복 누적 방지).
        // 결제 확정은 orderUid(주문 1개) 단위라, 새 주문 발급 전 이전 미결제 주문을 지워야 좀비 주문이 안 남는다.
        cleanupPendingGroupBuyOrders(participationSeq);

        Options option = optionsRepository.findById(optionsSeq)
                .orElseThrow(() -> new IllegalArgumentException("옵션을 찾을 수 없습니다. optionsSeq=" + optionsSeq));

        String productName = option.getProduct().getProductName(); // 구매 시점 상품명 스냅샷
        DeliveryInfo delivery = resolveDefaultDeliveryInfo(memberSeq);

        Orders order = Orders.builder()
                .memberSeq(memberSeq)
                .memberCouponSeq(null)
                .orderUid(createOrderUid())
                .productTotalPrice(originalPrice)
                .couponDiscount(0)
                .hotdealDiscount(0)
                .finalPrice(finalPrice)
                .totalRefundPrice(0)
                .remainPrice(finalPrice)
                .orderStatus(1)   // 결제대기
                .paymentStatus(0) // 미결제
                .orderDate(null)
                .regdate(LocalDateTime.now())
                .zipcode(delivery.zipcode())
                .address(delivery.address())
                .addressDetail(delivery.addressDetail())
                .currLatitude(DEFAULT_LATITUDE)
                .currLongitude(DEFAULT_LONGITUDE)
                .build();

        Orders savedOrder = ordersRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .orderSeq(savedOrder.getSeq())
                .participationSeq(participationSeq) // 공구 주문 식별 + 결제 후 확정 연결고리
                .optionsSeq(optionsSeq)
                .productName(productName)
                .quantity(1)
                .originalPrice(originalPrice)
                .hotdealDiscount(0)
                .couponDiscount(0)
                .participationDiscount(participationDiscount)
                .finalPrice(finalPrice)
                .subTotalPrice(finalPrice) // 수량 1
                .refundQuantity(0)
                .refundPrice(0)
                .itemStatus(0)
                .returnQuantity(null)
                .build();

        orderItemRepository.save(orderItem);

        return new OrderCreateResultDto(
                savedOrder.getSeq(),
                savedOrder.getOrderUid(),
                savedOrder.getFinalPrice(),
                productName
        );
    }

    /**
     * 같은 공구 참여에 남아 있는 미결제(paymentStatus=0) 결제대기 주문을 삭제한다.
     * 결제하기 재진입·이탈 후 재시도로 결제대기 주문이 중복 쌓이는 것을 막는다.
     * 미결제 주문은 결제·환불 이력이 없어 물리 삭제해도 안전하다(결제완료 주문은 건드리지 않는다).
     */
    private void cleanupPendingGroupBuyOrders(Long participationSeq) {
        for (OrderItem item : orderItemRepository.findByParticipationSeq(participationSeq)) {
            Orders order = ordersRepository.findById(item.getOrderSeq()).orElse(null);
            if (order != null && order.getPaymentStatus() != null && order.getPaymentStatus() == 0) {
                orderItemRepository.delete(item);
                ordersRepository.delete(order);
            }
        }
    }

    private DeliveryInfo resolveDefaultDeliveryInfo(Long memberSeq) {
        return memberAddressService.myAddress(memberSeq).stream()
                .filter(address -> "Y".equals(address.getDefaultYn()))
                .findFirst()
                .map(address -> new DeliveryInfo(
                        address.getZipcode(),
                        address.getAddress(),
                        address.getAddressDetail()))
                .orElseThrow(() -> new IllegalStateException("기본 배송지가 없습니다. 배송지를 먼저 등록해주세요."));
    }

    private void validateCheckoutRequest(CheckoutRequestDto requestDto,
                                         Long memberSeq) {

        if (memberSeq == null) {
            throw new IllegalArgumentException("로그인 회원 번호가 없습니다.");
        }

        if (Boolean.TRUE.equals(requestDto.getDirectBuyYn())) {
            if (requestDto.getOptionsSeq() == null) {
                throw new IllegalArgumentException("바로구매할 상품 옵션을 선택해야 합니다.");
            }

            if (requestDto.getQuantity() == null || requestDto.getQuantity() < 1) {
                throw new IllegalArgumentException("구매 수량은 1개 이상이어야 합니다.");
            }
        } else {
            if (requestDto.getCartSeq() == null || requestDto.getCartSeq().isEmpty()) {
                throw new IllegalArgumentException("결제할 장바구니 상품을 선택해야 합니다.");
            }
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