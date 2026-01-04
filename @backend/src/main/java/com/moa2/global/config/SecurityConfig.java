package com.moa2.global.config;

import com.moa2.global.handler.OAuth2FailureHandler;
import com.moa2.global.handler.OAuth2SuccessHandler;
import com.moa2.global.security.JwtAuthenticationFilter;
import com.moa2.global.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 * - OAuth2 로그인 설정
 * - JWT 인증 필터 설정
 * - 경로별 권한 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (API 서버이므로)
            .csrf(AbstractHttpConfigurer::disable)
            
            // 세션 정책 설정 (OAuth2 로그인 시 세션 사용, JWT 인증 시 STATELESS)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            
            // 경로별 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 공개 경로 (인증 불필요)
                .requestMatchers(
                    "/",
                    "/error",
                    "/oauth2/**",
                    "/login/**",
                    "/api/auth/verify",
                    "/api/auth/success",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                    "/api/auth/logout/complete",
                    "/api/auth/error"
                ).permitAll()
                
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(customAuthorizationRequestResolver())
                )
            )
            
            // JWT 인증 필터 추가 (OAuth2 필터 이후에 실행)
            .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    /**
     * OAuth2 인증 요청 리졸버 설정
     * prompt=select_account consent를 추가하여 항상 계정 선택 및 동의 화면이 표시되도록 함
     */
    private OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver() {
        OAuth2AuthorizationRequestResolver defaultResolver = 
            new org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
            );
        return new com.moa2.global.config.CustomOAuth2AuthorizationRequestResolver(defaultResolver);
    }
}

