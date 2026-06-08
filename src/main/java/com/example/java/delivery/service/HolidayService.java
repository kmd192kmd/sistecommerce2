package com.example.java.delivery.service;

import com.example.java.delivery.entity.Holiday;
import com.example.java.delivery.repository.HolidayRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final ObjectMapper objectMapper;

    @Value("${holiday.api.key:}")
    private String apiKey;

    /**
     * Checks if the given date is a non-business day (weekend or public holiday).
     */
    public boolean isNonBusinessDay(LocalDate date) {
        // Weekends check
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return true;
        }

        // DB에서 휴일 여부 확인 (캐싱된 데이터를 읽으므로 외부 API 의존성이 없고 매우 빠름)
        return holidayRepository.existsById(date);
    }

    /**
     * 서버 구동 시 DB에 공휴일 데이터가 하나도 없으면 즉시 동기화를 실행합니다.
     */
//    @EventListener(ApplicationReadyEvent.class)
//    public void initHolidaysOnStartup() {
//        if (holidayRepository.count() == 0) {
//            log.info("No holiday data found in DB. Starting initial sync...");
//            syncHolidays();
//        }
//    }

    /**
     * 매월 1일 새벽 4시에 금년과 내년도 공휴일 데이터를 공공데이터포털에서 가져와 DB에 동기화합니다.
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    public void syncHolidays() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("API Key for holidays is not set. Skipping sync.");
            return;
        }

        int currentYear = LocalDate.now().getYear();
        
        try {
            fetchAndSaveHolidays(currentYear);
            fetchAndSaveHolidays(currentYear + 1); // 내년 데이터까지 캐싱
            log.info("Successfully synced holidays for {} and {}", currentYear, currentYear + 1);
        } catch (Exception e) {
            log.error("Failed to sync holidays", e);
        }
    }

    private void fetchAndSaveHolidays(int year) throws Exception {
        StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo");
        urlBuilder.append("?" + URLEncoder.encode("serviceKey","UTF-8") + "=" + apiKey);
        urlBuilder.append("&" + URLEncoder.encode("solYear","UTF-8") + "=" + URLEncoder.encode(String.valueOf(year), "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=100"); // 1년치 공휴일을 한 번에 가져오기 위해 넉넉히 설정
        urlBuilder.append("&_type=json");

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            conn.disconnect();

            parseAndSave(sb.toString());
        } else {
            conn.disconnect();
            throw new RuntimeException("API Response failed: " + conn.getResponseCode());
        }
    }

    private void parseAndSave(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode items = root.path("response").path("body").path("items").path("item");

        List<Holiday> holidays = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        if (items.isArray()) {
            for (JsonNode item : items) {
                if ("Y".equals(item.path("isHoliday").asText())) {
                    String dateStr = item.path("locdate").asText();
                    String name = item.path("dateName").asText();
                    holidays.add(new Holiday(LocalDate.parse(dateStr, formatter), name));
                }
            }
        } else if (items.isObject()) { // 데이터가 1개일 경우 배열이 아닌 객체로 응답될 수 있음
            if ("Y".equals(items.path("isHoliday").asText())) {
                String dateStr = items.path("locdate").asText();
                String name = items.path("dateName").asText();
                holidays.add(new Holiday(LocalDate.parse(dateStr, formatter), name));
            }
        }

        // DB에 저장
        if (!holidays.isEmpty()) {
            holidayRepository.saveAll(holidays);
        }
    }
}
