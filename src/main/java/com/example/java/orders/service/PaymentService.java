package com.example.java.orders.service;

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
    private final ObjectMapper objectMapper;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    /**
     * 토스 successUrl에서 받은 paymentKey, orderId, amount로 결제 승인 요청을 처리한다.
     */
    @Transactional
    public void confirmPayment(String paymentKey,
                               String orderId,
                               Integer amount) {

        Orders order = ordersRepository.findByOrderUid(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderId=" + orderId));

        /*
            이미 결제 완료된 주문이면 중복 처리 방지.
            사용자가 successUrl을 새로고침할 수 있으므로 필요하다.
         */
        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 2) {
            return;
        }

        /*
            서버 DB 기준 금액과 토스에서 돌아온 amount를 비교한다.
            다르면 승인 API 호출 금지.
         */
        if (!order.getFinalPrice().equals(amount)) {
            order.setPaymentStatus(3); // 결제실패
            ordersRepository.save(order);

            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        if (paymentRepository.findByExternalPaymentId(paymentKey).isPresent()) {
            return;
        }

        JsonNode tossResponse = requestTossConfirm(paymentKey, orderId, amount);

        String status = getText(tossResponse, "status");

        if (!"DONE".equals(status)) {
            order.setPaymentStatus(3); // 결제실패
            ordersRepository.save(order);

            throw new IllegalStateException("토스 결제 승인이 완료되지 않았습니다. status=" + status);
        }

        String method = getText(tossResponse, "method");
        String approvedAt = getText(tossResponse, "approvedAt");
        String receiptUrl = null;

        if (tossResponse.has("receipt") && tossResponse.get("receipt").has("url")) {
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

        /*
            결제 완료 처리.
            order_date는 주문 생성일이 아니라 결제 완료 시각으로 저장한다.
         */
        order.setOrderStatus(2);       // 결제완료
        order.setPaymentStatus(2);     // 결제완료
        order.setOrderDate(approvedDateTime);
        order.setRemainPrice(amount);

        ordersRepository.save(order);
    }

    /**
     * 결제 실패 리다이렉트 처리.
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

        order.setPaymentStatus(3); // 결제실패
        ordersRepository.save(order);

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
                .failReason("[" + code + "] " + message)
                .updateDate(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
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

            String authorization = createBasicAuthorizationHeader();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOSS_CONFIRM_URL))
                    .header("Authorization", authorization)
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
                String message = responseBody.has("message")
                        ? responseBody.get("message").asText()
                        : response.body();

                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(response.statusCode()),
                        message
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

    private String createBasicAuthorizationHeader() {
        /*
            토스 API 인증은 secretKey + ":" 문자열을 Base64 인코딩해서 Basic 헤더로 보낸다.
         */
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
}