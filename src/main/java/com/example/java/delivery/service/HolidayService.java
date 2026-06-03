package com.example.java.delivery.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HolidayService {

    @Value("${holiday.api.key:}")
    private String apiKey;

    // Hardcoded fallback list of public holidays in Korea (solar dates + lunar dates converted for 2026)
    private static final Set<LocalDate> STATIC_HOLIDAYS_2026 = new HashSet<>();

    static {
        // Solar holidays
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 1, 1));   // 신정
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 3, 1));   // 삼일절
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 5, 5));   // 어린이날
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 6, 6));   // 현충일
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 8, 15));  // 광복절
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 10, 3));  // 개천절
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 10, 9));  // 한글날
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 12, 25)); // 성탄절

        // Lunar holidays in 2026 (Solar date equivalents)
        // 설날 연휴 (2026년 2월 16일 ~ 2월 18일)
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 2, 16));
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 2, 17));
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 2, 18));
        
        // 석가탄신일 (2026년 5월 24일) - 일요일이므로 대체공휴일 5월 25일 발생
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 5, 24));
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 5, 25));

        // 추석 연휴 (2026년 9월 24일 ~ 9월 26일)
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 9, 24));
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 9, 25));
        STATIC_HOLIDAYS_2026.add(LocalDate.of(2026, 9, 26));
    }

    /**
     * Checks if the given date is a non-business day (weekend or public holiday).
     */
    public boolean isNonBusinessDay(LocalDate date) {
        // Weekends check
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return true;
        }

        // Try calling OpenAPI first, if API key is present
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                if (checkHolidayViaApi(date)) {
                    return true;
                }
            } catch (Exception e) {
                // Fallback to static lists
                return checkFallbackHoliday(date);
            }
        }

        return checkFallbackHoliday(date);
    }

    private boolean checkFallbackHoliday(LocalDate date) {
        if (date.getYear() == 2026) {
            return STATIC_HOLIDAYS_2026.contains(date);
        }
        // Basic solar holidays for other years
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        if (month == 1 && day == 1) return true;
        if (month == 3 && day == 1) return true;
        if (month == 5 && day == 5) return true;
        if (month == 6 && day == 6) return true;
        if (month == 8 && day == 15) return true;
        if (month == 10 && day == 3) return true;
        if (month == 10 && day == 9) return true;
        if (month == 12 && day == 25) return true;
        return false;
    }

    /**
     * Call Open API (data.go.kr - 한국천문연구원 특일 정보)
     */
    private boolean checkHolidayViaApi(LocalDate date) throws Exception {
        String year = String.valueOf(date.getYear());
        String month = String.format("%02d", date.getMonthValue());
        
        StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo");
        urlBuilder.append("?" + URLEncoder.encode("serviceKey","UTF-8") + "=" + apiKey);
        urlBuilder.append("&" + URLEncoder.encode("solYear","UTF-8") + "=" + URLEncoder.encode(year, "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("solMonth","UTF-8") + "=" + URLEncoder.encode(month, "UTF-8"));
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

            String response = sb.toString();
            // Parse JSON to find if the date is in the holiday list
            String dateStr = String.format("%d%02d%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
            return response.contains(dateStr) && response.contains("\"isHoliday\":\"Y\"");
        }
        
        conn.disconnect();
        throw new RuntimeException("API Response failed");
    }
}
