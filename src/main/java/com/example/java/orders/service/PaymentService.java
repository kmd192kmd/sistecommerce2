package com.example.java.orders.service;

import com.example.java.cart.repository.CartRepository;
import com.example.java.delivery.entity.Delivery;
import com.example.java.delivery.repository.DeliveryRepository;
import com.example.java.member.entity.MemberCoupon;
import com.example.java.member.repository.MemberCouponRepository;
import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.Orders;
import com.example.java.orders.entity.Payment;
import com.example.java.orders.entity.Refund;
import com.example.java.orders.repository.OrderItemRepository;
import com.example.java.orders.repository.OrdersRepository;
import com.example.java.orders.repository.PaymentRepository;
import com.example.java.orders.repository.RefundRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final RefundRepository refundRepository;
    private final ObjectMapper objectMapper;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_PAYMENT_URL = "https://api.tosspayments.com/v1/payments";

    /**
     * 토스 결제 성공 후 호출되는 최종 승인 처리.
     */
    @Transactional
    public void confirmPayment(String paymentKey,
                               String orderId,
                               Integer amount) {

        Orders order = ordersRepository.findByOrderUid(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderId=" + orderId));

        /*
            이미 결제 완료된 주문이면 중복 처리하지 않는다.
            토스 성공 URL 새로고침, 브라우저 재요청 등에 대비한 처리다.
         */
        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 2) {
            return;
        }

        /*
            서버 DB에 저장된 최종 결제금액과 토스에서 돌아온 amount를 반드시 비교한다.
            화면 hidden amount 값은 사용자가 조작할 수 있으므로 신뢰하면 안 된다.
         */
        if (order.getFinalPrice() == null || !order.getFinalPrice().equals(amount)) {
            order.setPaymentStatus(3);
            ordersRepository.save(order);

            throw new IllegalArgumentException(
                    "결제 금액이 일치하지 않습니다. DB finalPrice="
                            + order.getFinalPrice()
                            + ", toss amount="
                            + amount
            );
        }

        /*
            같은 paymentKey로 이미 저장된 결제내역이 있으면 중복 저장하지 않는다.
         */
        if (paymentRepository.findByExternalPaymentId(paymentKey).isPresent()) {
            return;
        }

        JsonNode tossResponse = requestTossConfirm(paymentKey, orderId, amount);

        String status = getText(tossResponse, "status");

        if (!"DONE".equals(status)) {
            order.setPaymentStatus(3);
            ordersRepository.save(order);

            throw new IllegalStateException("토스 결제 승인이 완료되지 않았습니다. status=" + status);
        }

        String method = getText(tossResponse, "method");
        String approvedAt = getText(tossResponse, "approvedAt");

        String receiptUrl = null;

        if (tossResponse.has("receipt")
                && tossResponse.get("receipt") != null
                && tossResponse.get("receipt").has("url")) {
            receiptUrl = tossResponse.get("receipt").get("url").asText();
        }

        LocalDateTime approvedDateTime = parseTossDateTime(approvedAt);

        Payment payment = Payment.builder()
                .orderSeq(order.getSeq())
                .paymentUid(createPaymentUid())
                .externalPaymentId(paymentKey)
                .pgTid(paymentKey)
                .paymentMethod(mapPaymentMethod(method))
                .pgProvider("TOSS")
                .status(2)
                .amount(amount)
                .requestDate(LocalDateTime.now())
                .payDate(approvedDateTime)
                .receiptUrl(receiptUrl)
                .failReason(null)
                .updateDate(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        order.setOrderStatus(2);
        order.setPaymentStatus(2);
        order.setOrderDate(approvedDateTime);
        order.setRemainPrice(amount);

        ordersRepository.save(order);

        /*
            결제 완료 후 사용한 회원 쿠폰을 사용완료 처리한다.
         */
        if (order.getMemberCouponSeq() != null) {
            MemberCoupon memberCoupon = memberCouponRepository.findById(order.getMemberCouponSeq())
                    .orElseThrow(() -> new IllegalStateException(
                            "회원 쿠폰 정보를 찾을 수 없습니다. memberCouponSeq=" + order.getMemberCouponSeq()
                    ));

            if (memberCoupon.getStatus() != null && memberCoupon.getStatus() == 0) {
                memberCoupon.use();
            }
        }

        /*
            결제 성공 후 주문에 포함된 상품만 장바구니에서 삭제한다.
         */
        deleteOrderedCartItems(order);
    }

    /**
     * 기존 전체 주문 취소용 메서드.
     *
     * 내부적으로 아직 취소되지 않은 모든 order_item을 선택해서
     * cancelOrderItems()를 호출한다.
     */
    @Transactional
    public void cancelPayment(Long orderSeq,
                              Long memberSeq) {

        if (orderSeq == null) {
            throw new IllegalArgumentException("주문번호가 없습니다.");
        }

        Orders order = ordersRepository.findById(orderSeq)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderSeq=" + orderSeq));

        List<Long> orderItemSeqList = orderItemRepository.findByOrderSeq(order.getSeq())
                .stream()
                .filter(item -> item.getItemStatus() == null || item.getItemStatus() != 6)
                .map(OrderItem::getSeq)
                .toList();

        if (orderItemSeqList.isEmpty()) {
            throw new IllegalStateException("취소할 수 있는 주문상품이 없습니다.");
        }

        cancelOrderItems(orderSeq, memberSeq, orderItemSeqList, "고객 주문 취소");
    }

    /**
     * 선택한 주문상품만 부분취소한다.
     *
     * 현재 기준:
     * - 상품 라인 단위 부분취소
     * - 수량 일부 취소는 아직 지원하지 않음
     * - 선택한 order_item 전체 수량을 취소함
     *
     * 취소 가능 조건:
     * 1. 로그인 회원 본인의 주문
     * 2. 결제완료 또는 부분환불 상태의 주문
     * 3. 배송 상태가 READY 또는 배송 row 미생성 상태
     * 4. 아직 취소되지 않은 order_item
     */
    @Transactional
    public void cancelOrderItems(Long orderSeq,
                                 Long memberSeq,
                                 List<Long> orderItemSeqList,
                                 String cancelReason) {

        if (orderSeq == null) {
            throw new IllegalArgumentException("주문번호가 없습니다.");
        }

        if (memberSeq == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (orderItemSeqList == null || orderItemSeqList.isEmpty()) {
            throw new IllegalArgumentException("취소할 상품을 선택해야 합니다.");
        }

        String reason = isBlank(cancelReason) ? "고객 부분 주문 취소" : cancelReason;

        Orders order = ordersRepository.findById(orderSeq)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderSeq=" + orderSeq));

        if (!order.getMemberSeq().equals(memberSeq)) {
            throw new IllegalArgumentException("본인의 주문만 취소할 수 있습니다.");
        }

        if (order.getOrderStatus() != null && order.getOrderStatus() == 9) {
            throw new IllegalStateException("이미 전체 취소된 주문입니다.");
        }

        /*
            payment_status
            2: 결제완료
            4: 부분환불
            6: 결제취소
         */
        if (order.getPaymentStatus() == null
                || !(order.getPaymentStatus() == 2 || order.getPaymentStatus() == 4)) {
            throw new IllegalStateException("결제완료 또는 부분환불 상태의 주문만 취소할 수 있습니다.");
        }

        /*
            배송대기 상태에서만 취소 가능.
            현재 delivery가 order_item과 직접 연결되어 있지 않으므로,
            해당 주문의 모든 delivery row가 READY일 때만 허용한다.
         */
        validateDeliveryReady(order.getSeq());

        Payment payment = paymentRepository.findTopByOrderSeqAndStatusOrderBySeqDesc(order.getSeq(), 2)
                .orElseThrow(() -> new IllegalStateException("결제완료 정보를 찾을 수 없습니다. orderSeq=" + order.getSeq()));

        if (payment.getExternalPaymentId() == null || payment.getExternalPaymentId().isBlank()) {
            throw new IllegalStateException("토스 paymentKey가 없어 결제를 취소할 수 없습니다.");
        }

        Set<Long> distinctOrderItemSeqSet = new LinkedHashSet<>(orderItemSeqList);

        List<OrderItem> allOrderItems = orderItemRepository.findByOrderSeq(order.getSeq());

        List<OrderItem> cancelItems = allOrderItems.stream()
                .filter(item -> distinctOrderItemSeqSet.contains(item.getSeq()))
                .toList();

        if (cancelItems.size() != distinctOrderItemSeqSet.size()) {
            throw new IllegalArgumentException("선택한 상품 중 해당 주문에 포함되지 않은 상품이 있습니다.");
        }

        for (OrderItem item : cancelItems) {
            if (item.getItemStatus() != null && item.getItemStatus() == 6) {
                throw new IllegalStateException("이미 취소된 상품이 포함되어 있습니다. orderItemSeq=" + item.getSeq());
            }

            int quantity = nullToZero(item.getQuantity());
            int refundQuantity = nullToZero(item.getRefundQuantity());

            if (quantity <= 0) {
                throw new IllegalStateException("취소할 수량이 없는 상품이 포함되어 있습니다. orderItemSeq=" + item.getSeq());
            }

            if (refundQuantity >= quantity) {
                throw new IllegalStateException("이미 전량 환불된 상품이 포함되어 있습니다. orderItemSeq=" + item.getSeq());
            }
        }

        int cancelAmount = calculateCancelAmount(cancelItems);

        if (cancelAmount <= 0) {
            throw new IllegalStateException("취소할 금액이 없습니다.");
        }

        int remainPrice = getRemainPrice(order);

        if (cancelAmount > remainPrice) {
            throw new IllegalStateException(
                    "취소 금액이 남은 결제금액보다 큽니다. cancelAmount="
                            + cancelAmount
                            + ", remainPrice="
                            + remainPrice
            );
        }

        /*
            Toss 부분취소 API 호출.
            cancelAmount를 보내므로 선택 상품 금액만 취소된다.
         */
        requestTossCancel(payment.getExternalPaymentId(), reason, cancelAmount);

        LocalDateTime now = LocalDateTime.now();

        /*
            refund 테이블에 order_item별 환불 기록 저장.
         */
        List<Refund> refunds = cancelItems.stream()
                .map(item -> createRefundEntity(item, payment, now))
                .toList();

        refundRepository.saveAll(refunds);

        /*
            order_item 상태 갱신.
            선택한 상품 라인 전체 수량을 취소 처리한다.
         */
        for (OrderItem item : cancelItems) {
            item.setRefundQuantity(item.getQuantity());
            item.setRefundPrice(item.getSubTotalPrice());
            item.setItemStatus(6);
        }

        orderItemRepository.saveAll(cancelItems);

        /*
            orders 금액 및 상태 갱신.
         */
        int newTotalRefundPrice = nullToZero(order.getTotalRefundPrice()) + cancelAmount;
        int newRemainPrice = nullToZero(order.getFinalPrice()) - newTotalRefundPrice;

        if (newRemainPrice < 0) {
            newRemainPrice = 0;
        }

        order.setTotalRefundPrice(newTotalRefundPrice);
        order.setRemainPrice(newRemainPrice);

        boolean allItemsCanceled = isAllItemsCanceled(allOrderItems, distinctOrderItemSeqSet);

        if (allItemsCanceled) {
            /*
                모든 주문상품이 취소된 경우.
             */
            order.setOrderStatus(9);
            order.setPaymentStatus(6);

            payment.setStatus(4);
            payment.setFailReason(reason);

            /*
                전체 취소일 때만 쿠폰 복구.
                부분취소에서는 남은 상품에 쿠폰이 계속 적용된 상태로 유지한다.
             */
            restoreCouponIfExists(order);

            /*
                배송 row가 이미 있다면 실패 처리.
             */
            cancelDeliveries(order.getSeq());

        } else {
            /*
                일부 상품만 취소된 경우.
                order_status = 7 : 부분환불 또는 부분취소
                payment_status = 4 : 부분환불
             */
            order.setOrderStatus(7);
            order.setPaymentStatus(4);
        }

        payment.setUpdateDate(now);

        ordersRepository.save(order);
        paymentRepository.save(payment);
    }

    /**
     * 결제 실패 URL로 돌아온 경우 주문과 결제 실패 기록을 남긴다.
     */
    @Transactional
    public void markPaymentFail(String orderId,
                                String code,
                                String message) {

        if (orderId == null || orderId.isBlank()) {
            return;
        }

        Orders order = ordersRepository.findByOrderUid(orderId)
                .orElse(null);

        if (order == null) {
            return;
        }

        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 2) {
            return;
        }

        order.setPaymentStatus(3);
        ordersRepository.save(order);

        String failReason = "[" + nullToEmpty(code) + "] " + nullToEmpty(message);

        Payment payment = Payment.builder()
                .orderSeq(order.getSeq())
                .paymentUid(createPaymentUid())
                .externalPaymentId(null)
                .pgTid(null)
                .paymentMethod(0)
                .pgProvider("TOSS")
                .status(3)
                .amount(order.getFinalPrice())
                .requestDate(LocalDateTime.now())
                .payDate(null)
                .receiptUrl(null)
                .failReason(failReason)
                .updateDate(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
    }

    private void validateDeliveryReady(Long orderSeq) {
        List<Delivery> deliveries = deliveryRepository.findByOrders_Seq(orderSeq);

        if (deliveries == null || deliveries.isEmpty()) {
            return;
        }

        boolean allReady = deliveries.stream()
                .allMatch(delivery -> "READY".equals(delivery.getStatus()));

        if (!allReady) {
            throw new IllegalStateException("배송대기 상태의 주문만 취소할 수 있습니다.");
        }
    }

    private int calculateCancelAmount(List<OrderItem> cancelItems) {
        return cancelItems.stream()
                .mapToInt(item -> nullToZero(item.getSubTotalPrice()) - nullToZero(item.getRefundPrice()))
                .sum();
    }

    private Refund createRefundEntity(OrderItem item,
                                      Payment payment,
                                      LocalDateTime now) {

        int quantity = nullToZero(item.getQuantity());
        int alreadyRefundQuantity = nullToZero(item.getRefundQuantity());
        int cancelQuantity = quantity - alreadyRefundQuantity;

        if (cancelQuantity <= 0) {
            cancelQuantity = quantity;
        }

        int refundPrice = nullToZero(item.getSubTotalPrice()) - nullToZero(item.getRefundPrice());

        if (refundPrice < 0) {
            refundPrice = 0;
        }

        /*
            refund_product_price는 상품 원가 기준 환불 대상 금액.
            original_price가 1개 단가이므로 취소 수량을 곱한다.
         */
        int refundProductPrice = nullToZero(item.getOriginalPrice()) * cancelQuantity;

        return Refund.builder()
                .orderItemSeq(item.getSeq())
                .paymentSeq(payment.getSeq())
                .returnRequest(null)
                .returnsSeq(null)
                .refundUid(createRefundUid(item.getSeq()))
                .refundQuantity(cancelQuantity)
                .refundProductPrice(refundProductPrice)
                .refundHotdeal(nullToZero(item.getHotdealDiscount()))
                .refundCoupon(nullToZero(item.getCouponDiscount()))
                .refundParticipation(nullToZero(item.getParticipationDiscount()))
                .refundPrice(refundPrice)
                .status(2)
                .requestDate(now)
                .completeDate(now)
                .updateDate(now)
                .build();
    }

    private boolean isAllItemsCanceled(List<OrderItem> allOrderItems,
                                       Set<Long> currentCancelItemSeqSet) {

        for (OrderItem item : allOrderItems) {
            boolean alreadyCanceled = item.getItemStatus() != null && item.getItemStatus() == 6;
            boolean nowCanceled = currentCancelItemSeqSet.contains(item.getSeq());

            if (!alreadyCanceled && !nowCanceled) {
                return false;
            }
        }

        return true;
    }

    private int getRemainPrice(Orders order) {
        if (order.getRemainPrice() != null) {
            return order.getRemainPrice();
        }

        return nullToZero(order.getFinalPrice()) - nullToZero(order.getTotalRefundPrice());
    }

    private void restoreCouponIfExists(Orders order) {
        if (order.getMemberCouponSeq() == null) {
            return;
        }

        memberCouponRepository.findById(order.getMemberCouponSeq())
                .ifPresent(memberCoupon -> memberCoupon.updateStatus(0));
    }

    private void cancelDeliveries(Long orderSeq) {
        List<Delivery> deliveries = deliveryRepository.findByOrders_Seq(orderSeq);

        if (deliveries == null || deliveries.isEmpty()) {
            return;
        }

        for (Delivery delivery : deliveries) {
            delivery.setStatus("FAILED");
        }

        deliveryRepository.saveAll(deliveries);
    }

    private void deleteOrderedCartItems(Orders order) {
        List<Long> orderedOptionsSeqList = orderItemRepository.findByOrderSeq(order.getSeq())
                .stream()
                .map(OrderItem::getOptionsSeq)
                .distinct()
                .toList();

        if (orderedOptionsSeqList.isEmpty()) {
            return;
        }

        cartRepository.deleteByMember_SeqAndOptions_SeqIn(
                order.getMemberSeq(),
                orderedOptionsSeqList
        );
    }

    private JsonNode requestTossConfirm(String paymentKey,
                                        String orderId,
                                        Integer amount) {

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("paymentKey", paymentKey);
            body.put("orderId", orderId);
            body.put("amount", amount);

            String requestBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOSS_CONFIRM_URL))
                    .header("Authorization", createBasicAuthorizationHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode responseBody = objectMapper.readTree(response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorMessage = responseBody.has("message")
                        ? responseBody.get("message").asText()
                        : response.body();

                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(response.statusCode()),
                        errorMessage
                );
            }

            return responseBody;

        } catch (IOException e) {
            throw new IllegalStateException("토스 결제 승인 응답 처리 중 오류가 발생했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("토스 결제 승인 요청이 중단되었습니다.", e);
        }
    }

    /**
     * 토스 부분취소/전체취소 API 호출.
     *
     * cancelAmount를 보내면 부분취소,
     * 선택한 상품들이 주문 전체라면 결과적으로 전체취소가 된다.
     */
    private JsonNode requestTossCancel(String paymentKey,
                                       String cancelReason,
                                       Integer cancelAmount) {

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("cancelReason", cancelReason);
            body.put("cancelAmount", cancelAmount);

            String requestBody = objectMapper.writeValueAsString(body);

            String encodedPaymentKey = URLEncoder.encode(paymentKey, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOSS_PAYMENT_URL + "/" + encodedPaymentKey + "/cancel"))
                    .header("Authorization", createBasicAuthorizationHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode responseBody = objectMapper.readTree(response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorMessage = responseBody.has("message")
                        ? responseBody.get("message").asText()
                        : response.body();

                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(response.statusCode()),
                        errorMessage
                );
            }

            return responseBody;

        } catch (IOException e) {
            throw new IllegalStateException("토스 결제 취소 응답 처리 중 오류가 발생했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("토스 결제 취소 요청이 중단되었습니다.", e);
        }
    }

    private String createBasicAuthorizationHeader() {
        String value = tossSecretKey + ":";
        String encoded = Base64.getEncoder()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));

        return "Basic " + encoded;
    }

    private Integer mapPaymentMethod(String method) {
        if (method == null) {
            return 0;
        }

        return switch (method) {
            case "카드" -> 0;
            case "계좌이체" -> 1;
            case "가상계좌" -> 2;
            default -> 0;
        };
    }

    private LocalDateTime parseTossDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }

        return OffsetDateTime.parse(value).toLocalDateTime();
    }

    private String getText(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }

        return node.get(fieldName).asText();
    }

    private String createPaymentUid() {
        return "PAY-" + System.currentTimeMillis();
    }

    private String createRefundUid(Long orderItemSeq) {
        return "REF-" + System.currentTimeMillis() + "-" + orderItemSeq;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}