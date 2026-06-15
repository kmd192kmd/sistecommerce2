package com.example.java.chat.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.java.delivery.entity.Delivery;
import com.example.java.delivery.repository.DeliveryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.PropertySource("classpath:application-secret.yml")
public class AiChatbotService {

    private final DeliveryRepository deliveryRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 🚨 환경변수 'OPENAI_API_KEY'를 최우선으로 읽고, 
    // 없으면 application-secret.yml의 값을 읽도록 설정합니다.
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String apiModel;
    
    @PostConstruct
    public void checkKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("❌ API 키가 설정되지 않았습니다. 환경변수 OPENAI_API_KEY를 설정하세요.");
        } else {
            log.info("✅ API 키 로드 성공 (키 길이: {})", apiKey.trim().length());
        }
    }

    @Transactional(readOnly = true)
    public String processUserMessageAndRespond(Long roomSeq, Long memberSeq, String message) {
        log.info("채팅 메시지 처리 시작(OpenAI) - 방번호: {}, 발신자: {}, 내용: {}", roomSeq, memberSeq, message);
        
        StringBuilder systemPrompt = new StringBuilder("당신은 쇼핑몰의 전문적이고 친절한 CS 상담 AI 어시스턴트입니다. ");
        
        if (message != null && message.contains("배송")) {
            List<Delivery> deliveries = deliveryRepository.findTop1ByOrders_MemberSeqOrderByDispatchAtDesc(memberSeq, PageRequest.of(0, 1));
            
            if (deliveries != null && !deliveries.isEmpty()) {
                Delivery latestDelivery = deliveries.get(0);
                String deliveryInfo = String.format(
                    "최근 배송 상태: %s, 택배사: %s, 운송장번호: %s, 발송일시: %s, 예상배송일: %s",
                    latestDelivery.getStatus(),
                    latestDelivery.getDeliveryCompany() != null ? latestDelivery.getDeliveryCompany().getName() : "정보 없음",
                    latestDelivery.getTrackingNumber() != null ? latestDelivery.getTrackingNumber() : "미발급",
                    latestDelivery.getDispatch_at(),
                    latestDelivery.getEstimated_date()
                );
                systemPrompt.append("\n[참고 데이터] ").append(deliveryInfo);
            }
        }

        try {
            return callOpenAiApi(systemPrompt.toString(), message);
        } catch (Exception e) {
            log.error("OpenAI 연동 오류 발생: ", e);
            return "죄송합니다. 현재 AI 서버 연결에 문제가 발생했습니다.";
        }
    }

    private String callOpenAiApi(String systemPrompt, String userMessage) {
        if (apiKey == null || apiKey.isEmpty()) throw new RuntimeException("API 키가 설정되지 않았습니다.");
        log.info("🔥 [보안 테스트] 전송 전 API 키 전체 길이: {}, 키 앞 10자리: {}", 
                apiKey.trim().length(), apiKey.trim().substring(0, 10));
        log.info("🚀 최종 인증 헤더값 확인: Bearer " + apiKey.trim());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 환경변수에서 가져온 키를 안전하게 trim()하여 사용
        headers.set("Authorization", "Bearer " + apiKey.trim());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", apiModel);
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));
        requestBody.put("messages", messages);

        try {
            String jsonRequest = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("OpenAI API 통신 오류", e);
        }
    }
}