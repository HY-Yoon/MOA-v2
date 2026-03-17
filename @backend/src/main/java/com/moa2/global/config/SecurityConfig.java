package com.moa2.global.config;

import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.handler.OAuth2FailureHandler;
import com.moa2.global.handler.OAuth2SuccessHandler;
import com.moa2.global.security.JwtAuthenticationEntryPoint;
import com.moa2.global.security.JwtTokenProvider;
import com.moa2.global.security.JwtAuthenticationFilter;
import com.moa2.global.security.QueueTokenFilter;
import com.moa2.api.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정
 * - OAuth2 로그인 설정
 * - JWT 인증 필터 설정
 * - 경로별 권한 설정
 * - 환경별 보안 설정 (로컬/배포)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;
        private final OAuth2FailureHandler oAuth2FailureHandler;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final JwtTokenProvider jwtTokenProvider;
        private final UserRepository userRepository;
        private final ClientRegistrationRepository clientRegistrationRepository;

        // V2 프로필에서만 주입됨 (V1에서는 null)
        @org.springframework.beans.factory.annotation.Autowired(required = false)
        private QueueTokenFilter queueTokenFilter;

        // CORS 설정
        @Value("${security.cors.allowed-origins}")
        private List<String> allowedOrigins;

        @Value("${security.cors.allowed-methods}")
        private List<String> allowedMethods;

        @Value("${security.cors.allowed-headers}")
        private List<String> allowedHeaders;

        @Value("${security.cors.exposed-headers}")
        private List<String> exposedHeaders;

        @Value("${security.cors.allow-credentials}")
        private boolean allowCredentials;

        @Value("${security.cors.max-age}")
        private long maxAge;

        // 보안 헤더 설정
        @Value("${security.headers.xss-protection.enabled}")
        private boolean xssProtectionEnabled;

        @Value("${security.headers.frame-options.enabled}")
        private boolean frameOptionsEnabled;

        @Value("${security.headers.content-type-options.enabled}")
        private boolean contentTypeOptionsEnabled;

        @Value("${security.headers.hsts.enabled}")
        private boolean hstsEnabled;

        @Value("${security.headers.hsts.max-age}")
        private long hstsMaxAge;

        @Value("${security.headers.hsts.include-subdomains}")
        private boolean hstsIncludeSubDomains;

        @Value("${security.headers.hsts.preload}")
        private boolean hstsPreload;

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // 허용할 Origin (환경변수에서 주입)
                configuration.setAllowedOrigins(allowedOrigins);

                // 허용할 HTTP 메서드
                configuration.setAllowedMethods(allowedMethods);

                // 허용할 헤더
                configuration.setAllowedHeaders(allowedHeaders);

                // 쿠키 전송 허용
                configuration.setAllowCredentials(allowCredentials);

                // 브라우저가 응답 헤더를 읽을 수 있도록 노출
                configuration.setExposedHeaders(exposedHeaders);

                // Preflight 요청 캐싱 시간
                configuration.setMaxAge(maxAge);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                // CustomOAuth2AuthorizationRequestResolver 생성
                // 기존 회원은 동의 화면 건너뛰고, 신규 회원만 동의 화면 표시
                DefaultOAuth2AuthorizationRequestResolver defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                                clientRegistrationRepository, "/oauth2/authorization");
                CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver = new CustomOAuth2AuthorizationRequestResolver(
                                defaultResolver, jwtTokenProvider, userRepository);
                http
                                // CORS 적용
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // CSRF 비활성화 (API 서버이므로)
                                .csrf(AbstractHttpConfigurer::disable)

                                // 보안 헤더 설정
                                .headers(headers -> {
                                        // XSS 공격 방지
                                        if (xssProtectionEnabled) {
                                                headers.xssProtection(xss -> xss
                                                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK));
                                        }

                                        // 클릭재킹 방지
                                        if (frameOptionsEnabled) {
                                                headers.frameOptions(frame -> frame.deny());
                                        }

                                        // MIME 스니핑 방지
                                        if (contentTypeOptionsEnabled) {
                                                headers.contentTypeOptions(Customizer.withDefaults());
                                        }

                                        // HSTS (HTTP Strict Transport Security)
                                        if (hstsEnabled) {
                                                headers.httpStrictTransportSecurity(hsts -> hsts
                                                                .maxAgeInSeconds(hstsMaxAge)
                                                                .includeSubDomains(hstsIncludeSubDomains)
                                                                .preload(hstsPreload));
                                        }
                                })

                                // 세션 정책 설정 (OAuth2 로그인 시 세션 사용, JWT 인증 시 STATELESS)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

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
                                                                "/api/auth/exchange-code", // 일회용 코드 → JWT 토큰 교환 (로그인 직후 호출, 인증 불필요)
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**",
                                                                "/actuator/**",
                                                                "/actuator/health",
                                                                "/api/v1/shows/**",
                                                                "/api/v2/schedules/**",
                                                                "/api/test/**",
                                                                "/api/v1/payment/**")
                                                .permitAll()

                                                // 관리자 전용 경로 (ADMIN 역할 필요)
                                                .requestMatchers("/api/v1/admin/**")
                                                .hasRole("ADMIN")

                                                // 마이페이지 - 인증 필요
                                                .requestMatchers("/api/v1/users/**", "/api/v1/reservations/**")
                                                .authenticated()

                                                // 나머지 모든 요청은 인증 필요
                                                .anyRequest().authenticated())

                                // OAuth2 로그인 설정
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/api/auth/login")
                                                .authorizationEndpoint(authorization -> authorization
                                                                .authorizationRequestResolver(
                                                                                customOAuth2AuthorizationRequestResolver))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oAuth2SuccessHandler)
                                                .failureHandler(oAuth2FailureHandler))

                                // JWT 인증 필터 추가 (OAuth2 필터 이후에 실행)
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                // V2: 대기열 토큰 사전 검증 필터 (JWT 인증 후, 컨트롤러 전에 실행)
                if (queueTokenFilter != null) {
                        http.addFilterAfter(queueTokenFilter, JwtAuthenticationFilter.class);
                }

                http
                                // 인증 실패 시 401 처리
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint));

                return http.build();
        }
}
