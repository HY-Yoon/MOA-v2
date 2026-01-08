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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin (프론트엔드 주소)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 쿠키 전송 허용
        configuration.setAllowCredentials(true);
        
        // 브라우저가 응답 헤더를 읽을 수 있도록 노출
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie"));
        
        // Preflight 요청 캐싱 시간 (1시간)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
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
                    "/api/auth/login",
                    "/api/auth/verify",
                    "/api/auth/success",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                    "/api/auth/logout/complete",
                    "/api/auth/error",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                     "/v3/api-docs/**",
                        "/api/v1/admin/**"
                ).permitAll()
                
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                    .loginPage("/api/auth/login")
                    .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            
            // JWT 인증 필터 추가 (OAuth2 필터 이후에 실행)
            .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}

