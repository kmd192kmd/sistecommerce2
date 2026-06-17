package com.example.java.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync // 비동기(@Async) 처리를 가능하게 합니다.
public class AsyncConfig {
}