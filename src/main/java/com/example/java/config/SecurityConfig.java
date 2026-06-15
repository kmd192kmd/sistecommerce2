package com.example.java.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.java.member.security.CustomOAuth2UserService;
import com.example.java.member.security.CustomRememberMeServices;
import com.example.java.member.security.FormLoginFailureHandler;
import com.example.java.member.security.FormLoginSuccessHandler;
import com.example.java.member.security.OAuth2FailureHandler;
import com.example.java.member.security.OAuth2SuccessHandler;
import com.example.java.member.service.MemberSecurityService;
import com.example.java.member.service.RememberMeTokenService;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import com.example.java.admin.repository.AdminRepository;
import com.example.java.admin.entity.Admin;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final FormLoginSuccessHandler formLoginSuccessHandler;
    private final FormLoginFailureHandler formLoginFailureHandler;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final OAuth2FailureHandler oauth2FailureHandler;

    /**
     * remember-me 서명 키. services 와 rememberMe DSL 양쪽이 동일해야
     * RememberMeAuthenticationToken 검증이 통과한다.
     * 운영에서는 application-secret.yml 등으로 외부화 권장.
     */
    @Value("${security.remember-me.key:shop-remember-me-key-change-in-prod}")
    private String rememberMeKey;

    /** remember_me_token 기반 커스텀 자동로그인 서비스 */
    @Bean
    public CustomRememberMeServices customRememberMeServices(
            MemberSecurityService memberSecurityService,
            RememberMeTokenService rememberMeTokenService) {
        return new CustomRememberMeServices(rememberMeKey, memberSecurityService, rememberMeTokenService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CustomRememberMeServices customRememberMeServices) throws Exception {
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
                
                // 관리자 페이지는 ADMIN 권한 필요
                .requestMatchers("/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")

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
                .loginPage("/member/login")
                .loginProcessingUrl("/member/login")
                .successHandler(formLoginSuccessHandler)
                .failureHandler(formLoginFailureHandler)
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
            .rememberMe(rm -> rm
                .key(rememberMeKey)                              // services 와 동일 키 필수
                .rememberMeServices(customRememberMeServices)    // remember_me_token 기반 커스텀 서비스
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
        return new PasswordEncoder() {
            private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
            private final PasswordEncoder argon2 = org.springframework.security.crypto.argon2.Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

            @Override
            public String encode(CharSequence rawPassword) {
                return bcrypt.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (encodedPassword != null && encodedPassword.startsWith("$argon2")) {
                    return argon2.matches(rawPassword, encodedPassword);
                }
                return bcrypt.matches(rawPassword, encodedPassword);
            }
        };
    }

    @Bean
    public CommandLineRunner initAdminUser(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // "admin" 계정이 없으면 새로 만들고, 있으면 비밀번호를 bcrypt로 덮어씁니다.
            adminRepository.findByAdminId("admin").ifPresentOrElse(
                admin -> {
                    if (!passwordEncoder.matches("admin", admin.getPassword())) {
                        Admin updatedAdmin = Admin.builder()
                                .seq(admin.getSeq())
                                .id(admin.getId())
                                .password(passwordEncoder.encode("admin"))
                                .admRole(admin.getAdmRole())
                                .admStatus(admin.getAdmStatus())
                                .role(admin.getRole())
                                .build();
                        adminRepository.save(updatedAdmin);
                    }
                },
                () -> {
                    Admin newAdmin = Admin.builder()
                            .id("admin")
                            .password(passwordEncoder.encode("admin"))
                            .admRole(1)
                            .admStatus(0)
                            .role("ROLE_ADMIN")
                            .build();
                    adminRepository.save(newAdmin);
                }
            );
        };
    }
}
