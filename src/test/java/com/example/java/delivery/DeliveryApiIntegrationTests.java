package com.example.java.delivery;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.java.delivery.service.HolidayService;
import com.example.java.delivery.service.KakaoMapService;

@SpringBootTest
public class DeliveryApiIntegrationTests {

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private KakaoMapService kakaoMapService;

    @Test
    public void testHolidayApiConnection() {
        System.out.println("====== [공휴일 API 연동 테스트 시작] ======");
        
        // 2026-05-05 (어린이날 - 공휴일)
        LocalDate childrenDay = LocalDate.of(2026, 5, 5);
        boolean isHoliday = holidayService.isNonBusinessDay(childrenDay);
        System.out.println("2026년 5월 5일 (어린이날) 공휴일/휴일 판정 결과: " + isHoliday);

        // 2026-05-06 (평일)
        LocalDate weekday = LocalDate.of(2026, 5, 6);
        boolean isWeekdayHoliday = holidayService.isNonBusinessDay(weekday);
        System.out.println("2026년 5월 6일 (평일) 공휴일/휴일 판정 결과: " + isWeekdayHoliday);
        
        System.out.println("====== [공휴일 API 연동 테스트 완료] ======");
    }

    @Test
    public void testKakaoMapApiConnection() {
        System.out.println("====== [카카오맵 API 연동 테스트 시작] ======");
        
        // 본사허브(서울 대치동) 좌표
        double hqLat = 37.5049;
        double hqLon = 127.0505;
        
        // 중간허브1(경기 수원) 좌표
        double hubLat = 37.2635;
        double hubLon = 127.0286;

        double distance = kakaoMapService.getDrivingDistanceMeters(hqLat, hqLon, hubLat, hubLon);
        System.out.println("본사허브에서 경기 중간허브까지 계산된 거리: " + (distance / 1000.0) + " km (" + distance + " m)");
        
        System.out.println("====== [카카오맵 API 연동 테스트 완료] ======");
    }
}
