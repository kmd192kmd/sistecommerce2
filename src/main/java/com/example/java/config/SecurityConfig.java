package com.example.java.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스 허용 (/src/** : static/src/images/... 로컬 이미지)
                .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/src/**").permitAll()
                // 회원 관련 허용
                .requestMatchers("/member/login",
                                 "/member/signup/**", "/member/signup").permitAll()
                // 메인, 상품 목록 허용
                .requestMatchers("/", "/products/**", "/group-buys/**", "/hotdeals/**").permitAll()
                // 공구 조회 REST API 허용 (비회원도 조회 가능)
                .requestMatchers("/api/group-buys/**").permitAll()
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/member/login")         // 커스텀 로그인 페이지
                .loginProcessingUrl("/member/login") // POST 처리 URL
                .defaultSuccessUrl("/", true)        // 로그인 성공 시
                .failureUrl("/member/login?error")   // 로그인 실패 시
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/member/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .permitAll()
            )
            .csrf(csrf -> csrf.disable()); // 개발 중 CSRF 비활성화

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
