package com.example.java.admin.scheduler; // 패키지 경로는 생성하신 위치에 맞게 적어주세요.

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration      // 스프링에게 이 클래스가 '설정 파일'임을 알려줍니다.
@EnableScheduling   // 스케줄링 기능을 활성화합니다.
public class SchedulerConfig {

    // 내부는 텅 비워두셔도 됩니다!
    // 이 파일이 존재하는 것만으로도 스프링 부트 전체에 스케줄러가 켜지게 됩니다.

}