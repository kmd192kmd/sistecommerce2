package com.example.java.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.java.member.security.CustomOAuth2UserService;
import com.example.java.member.security.FormLoginSuccessHandler;
import com.example.java.member.security.OAuth2FailureHandler;
import com.example.java.member.security.OAuth2SuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final FormLoginSuccessHandler formLoginSuccessHandler;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final OAuth2FailureHandler oauth2FailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스 허용 (/src/** : static/src/images/... 로컬 이미지)
                .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/src/**").permitAll()
                // 회원 관련 허용
                .requestMatchers("/member/login", "/member/**",
                                 "/member/signup/**", "/member/signup").permitAll()
                // 메인, 상품 목록 허용
                .requestMatchers("/", "/products/**", "/group-buys/**", "/hotdeals/**").permitAll()
                // 공구 조회 REST API 허용 (비회원도 조회 가능)
                .requestMatchers("/api/group-buys/**").permitAll()
                // 나머지는 인증 필요
                //.anyRequest().authenticated()

                .requestMatchers("/payments/**").permitAll()

                //주문 및 마이페이지 로그인 필요
                .requestMatchers("/order/**", "/mypage/**").authenticated()

                // SSE 연결 (로그인 사용자만)
                .requestMatchers("/sse/connect").authenticated()

                // TODO 개발용으로 모두허용 (나중에 없애야)
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/member/login")         // 커스텀 로그인 페이지
                .loginProcessingUrl("/member/login") // POST 처리 URL
                .defaultSuccessUrl("/", false)        // 로그인 성공 시
                .failureUrl("/member/login?error")   // 로그인 실패 시
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/member/login")           // 소셜 로그인도 같은 로그인 페이지 사용
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(customOAuth2UserService) // Google은 OIDC 프로토콜 사용
                )
                .successHandler(oauth2SuccessHandler)
                .failureHandler(oauth2FailureHandler)
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
