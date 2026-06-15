package com.example.java.member.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.entity.Member;
import com.example.java.member.entity.Memberships;
import com.example.java.member.entity.MembershipsLog;
import com.example.java.member.repository.MembershipsLogRepository;
import com.example.java.member.repository.MembershipsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 멤버십 가입/취소/갱신/만료 처리 서비스.
 *
 * Toss 빌링 흐름:
 *  1. 프론트 → Toss 위젯에서 카드 등록 → authKey + customerKey 획득
 *  2. join() 호출 → 빌링키 발급 → 첫 결제 → DB 저장 + 이력 기록
 *  3. 매월 스케줄러에서 renew() 호출 → 자동 갱신
 *  4. 취소 요청 시 scheduleCancel() → 만료일까지 유지 후 expire() 처리
 *
 * 연쇄결제 방지:
 *  active / canceled 상태에서 join() 재호출 시 예외 발생.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private static final String TOSS_BILLING_ISSUE_URL = "https://api.tosspayments.com/v1/billing/authorizations/issue";
    private static final String TOSS_BILLING_URL       = "https://api.tosspayments.com/v1/billing/";
    public  static final int    MEMBERSHIP_PRICE       = 7900;

    private final MembershipsRepository    membershipsRepository;
    private final MembershipsLogRepository membershipsLogRepository;
    private final ObjectMapper             objectMapper;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    // -------------------------------------------------------------------------
    // 조회
    // -------------------------------------------------------------------------

    public Optional<Memberships> findByMember(Member member) {
        return membershipsRepository.findByMember(member);
    }

    public int getMembershipPrice() {
        return MEMBERSHIP_PRICE;
    }

    // -------------------------------------------------------------------------
    // 가입
    // -------------------------------------------------------------------------

    /**
     * 멤버십 가입.
     *
     * @param member      로그인 회원
     * @param authKey     Toss 위젯에서 반환한 authKey
     * @param customerKey 회원 고유 키 (member.seq 기반으로 프론트에서 생성)
     */
    @Transactional
    public void join(Member member, String authKey, String customerKey) {
        // 연쇄결제 방지: active / canceled 상태면 재가입 불가
        membershipsRepository.findByMember(member).ifPresent(m -> {
            if (Memberships.STATUS_ACTIVE.equals(m.getStatus())
                    || Memberships.STATUS_CANCELED.equals(m.getStatus())) {
                throw new IllegalStateException("이미 활성화된 멤버십이 있습니다.");
            }
        });

        int price = getMembershipPrice();

        // 1. 빌링키 발급
        String billingKey = issueBillingKey(authKey, customerKey);

        // 2. 첫 결제
        String orderId = "MEM-" + member.getSeq() + "-" + System.currentTimeMillis();
        chargeByBillingKey(billingKey, customerKey, price, orderId, "Gold Market 멤버십");

        // 3. DB 저장
        LocalDateTime now        = LocalDateTime.now();
        LocalDateTime expireAt   = now.plusMonths(1);
        LocalDateTime nextBilling = expireAt;

        Memberships memberships = membershipsRepository.findByMember(member)
                .orElse(Memberships.builder().member(member).build());

        memberships.activate(billingKey, expireAt, nextBilling);
        membershipsRepository.save(memberships);

        // 4. 이력 기록
        membershipsLogRepository.save(MembershipsLog.builder()
                .member(member)
                .memberships(memberships)
                .type(MembershipsLog.TYPE_JOIN)
                .amount(price)
                .build());

        log.info("멤버십 가입 완료 - member: {}, orderId: {}", member.getUsername(), orderId);
    }

    // -------------------------------------------------------------------------
    // 취소 예정
    // -------------------------------------------------------------------------

    /**
     * 취소 예정 등록.
     * 만료일까지 혜택 유지, 자동갱신 중단.
     */
    @Transactional
    public void scheduleCancel(Member member) {
        Memberships memberships = membershipsRepository.findByMember(member)
                .orElseThrow(() -> new IllegalStateException("멤버십 정보를 찾을 수 없습니다."));

        if (!Memberships.STATUS_ACTIVE.equals(memberships.getStatus())) {
            throw new IllegalStateException("활성화된 멤버십만 취소할 수 있습니다.");
        }

        memberships.scheduleCancel();
        membershipsRepository.save(memberships);

        membershipsLogRepository.save(MembershipsLog.builder()
                .member(member)
                .memberships(memberships)
                .type(MembershipsLog.TYPE_CANCEL)
                .amount(null)
                .build());

        log.info("멤버십 취소 예정 등록 - member: {}", member.getUsername());
    }

    // -------------------------------------------------------------------------
    // 만료 처리 (스케줄러용)
    // -------------------------------------------------------------------------

    @Transactional
    public void expire(Memberships memberships) {
        memberships.expire();
        membershipsRepository.save(memberships);

        membershipsLogRepository.save(MembershipsLog.builder()
                .member(memberships.getMember())
                .memberships(memberships)
                .type(MembershipsLog.TYPE_EXPIRE)
                .amount(null)
                .build());

        log.info("멤버십 만료 처리 - member: {}", memberships.getMember().getUsername());
    }

    // -------------------------------------------------------------------------
    // 자동 갱신 (스케줄러용)
    // -------------------------------------------------------------------------

    @Transactional
    public void renew(Memberships memberships) {
        int price     = getMembershipPrice();
        String orderId = "MEM-RNW-" + memberships.getMember().getSeq() + "-" + System.currentTimeMillis();
        String customerKey = "member-" + memberships.getMember().getSeq();

        chargeByBillingKey(memberships.getBillingKey(), customerKey, price, orderId, "Gold Market 멤버십 갱신");

        LocalDateTime expireAt    = LocalDateTime.now().plusMonths(1);
        LocalDateTime nextBilling = expireAt;
        memberships.activate(memberships.getBillingKey(), expireAt, nextBilling);
        membershipsRepository.save(memberships);

        membershipsLogRepository.save(MembershipsLog.builder()
                .member(memberships.getMember())
                .memberships(memberships)
                .type(MembershipsLog.TYPE_RENEW)
                .amount(price)
                .build());

        log.info("멤버십 갱신 완료 - member: {}", memberships.getMember().getUsername());
    }

    // -------------------------------------------------------------------------
    // Toss API 호출
    // -------------------------------------------------------------------------

    /**
     * Toss 빌링키 발급.
     * POST https://api.tosspayments.com/v1/billing/authorizations/issue
     */
    private String issueBillingKey(String authKey, String customerKey) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("authKey", authKey);
            body.put("customerKey", customerKey);

            JsonNode response = callTossApi(TOSS_BILLING_ISSUE_URL, body);

            if (!response.has("billingKey")) {
                throw new IllegalStateException("빌링키 발급 실패: " + response);
            }

            return response.get("billingKey").asText();

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IllegalStateException("빌링키 발급 중 오류 발생", e);
        }
    }

    /**
     * 빌링키로 결제.
     * POST https://api.tosspayments.com/v1/billing/{billingKey}
     */
    private void chargeByBillingKey(String billingKey, String customerKey,
                                    int amount, String orderId, String orderName) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("customerKey", customerKey);
            body.put("amount", amount);
            body.put("orderId", orderId);
            body.put("orderName", orderName);

            JsonNode response = callTossApi(TOSS_BILLING_URL + billingKey, body);

            String status = response.has("status") ? response.get("status").asText() : "";
            if (!"DONE".equals(status)) {
                throw new IllegalStateException("멤버십 결제 실패. status=" + status);
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IllegalStateException("멤버십 결제 중 오류 발생", e);
        }
    }

    private JsonNode callTossApi(String url, Map<String, Object> body)
            throws IOException, InterruptedException {

        String requestBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        JsonNode responseBody = objectMapper.readTree(response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String msg = responseBody.has("message")
                    ? responseBody.get("message").asText()
                    : response.body();
            throw new IllegalStateException("Toss API 오류: " + msg);
        }

        return responseBody;
    }

    private String basicAuthHeader() {
        String value   = tossSecretKey + ":";
        String encoded = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
