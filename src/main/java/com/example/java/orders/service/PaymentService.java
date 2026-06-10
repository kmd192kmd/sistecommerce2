package com.example.java.orders.service;

import com.example.java.cart.repository.CartRepository;
import com.example.java.member.entity.MemberCoupon;
import com.example.java.member.repository.MemberCouponRepository;
import com.example.java.orders.entity.Orders;
import com.example.java.orders.entity.Payment;
import com.example.java.orders.repository.OrdersRepository;
import com.example.java.orders.repository.PaymentRepository;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final CartRepository cartRepository;
    private final ObjectMapper objectMapper;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    /**
     * 토스 결제 성공 후 호출되는 최종 승인 처리.
     *
     * 흐름:
     * 1. orderId로 주문 조회
     * 2. DB 주문 금액과 토스 amount 비교
     * 3. 토스 confirm API 호출
     * 4. payment 저장
     * 5. orders 결제완료 처리
     * 6. 사용 쿠폰 상태 변경
     * 7. 장바구니 비우기
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

        /*
            payment 테이블 저장
         */
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

        /*
            orders 테이블 결제완료 처리
         */
        order.setOrderStatus(2);
        order.setPaymentStatus(2);
        order.setOrderDate(approvedDateTime);
        order.setRemainPrice(amount);

        ordersRepository.save(order);

        /*
            결제 완료 후 사용한 회원 쿠폰을 사용완료 처리한다.
            orders.member_coupon_seq에는 member_coupon.seq가 들어 있어야 한다.
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
            결제 성공 후 장바구니 비우기.
            현재 주문 구조는 '회원의 장바구니 전체 결제'이므로 해당 회원의 장바구니를 모두 삭제한다.
         */
        cartRepository.deleteByMember_Seq(order.getMemberSeq());
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

        /*
            이미 결제 완료된 주문이면 실패 처리하지 않는다.
         */
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

    /**
     * 토스 결제 승인 API 호출.
     */
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
     * Toss Secret Key를 Basic Auth 헤더로 변환.
     *
     * 형식:
     * Authorization: Basic Base64(secretKey + ":")
     */
    private String createBasicAuthorizationHeader() {
        String value = tossSecretKey + ":";
        String encoded = Base64.getEncoder()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));

        return "Basic " + encoded;
    }

    /**
     * 토스 method 문자열을 프로젝트 payment_method 코드로 변환.
     *
     * 프로젝트 정의에 맞게 숫자는 조정 가능.
     * 0: 카드
     * 1: 계좌이체
     * 2: 가상계좌
     */
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

    /**
     * 토스 approvedAt 예:
     * 2026-06-09T17:00:00+09:00
     */
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}