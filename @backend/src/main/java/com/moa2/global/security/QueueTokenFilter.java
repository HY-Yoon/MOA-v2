package com.moa2.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa2.global.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.lang.NonNull;

/**
 * V2: 대기열 토큰 사전 검증 필터
 * - /api/v2/reservations/** 경로에만 적용
 * - API 요청 시 QUEUE-TOKEN 쿠키의 토큰을 Redis에서 존재 여부만 확인
 * - 유효하지 않은 토큰은 Controller/DB에 도달하기 전에 즉시 400 반환
 * - DB 접근 없이 Redis 조회 1회로 가짜 트래픽을 빠르게 차단
 */
@Slf4j
@Profile("v2")
@Component
@RequiredArgsConstructor
public class QueueTokenFilter extends OncePerRequestFilter {

    private static final String QUEUE_TOKEN_COOKIE = "QUEUE-TOKEN";
    private static final String TOKEN_PREFIX = "token:";
    private static final String V2_RESERVATION_PATTERN = "/api/v2/reservations/**";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // CORS preflight(OPTIONS)는 토큰 검증 없이 통과
        if (HttpMethod.OPTIONS.matches(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // V2 예매 API 경로가 아니면 그냥 통과
        if (!pathMatcher.match(V2_RESERVATION_PATTERN, uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // [V2 흐름] 아래 경로는 /reserve 이후 단계로 토큰이 이미 소진됨 → 토큰 검증 불필요
        // /preview : 결제 페이지 진입 시 주문 미리보기 조회
        if (pathMatcher.match("/api/v2/reservations/preview/**", uri) ||
            pathMatcher.match("/api/v2/reservations/status/**", uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. QUEUE-TOKEN 쿠키 확인
        String token = extractQueueTokenFromCookie(request);
        if (token == null || token.isBlank()) {
            log.warn("🚨 [QueueTokenFilter] API 차단 - 'QUEUE-TOKEN' 쿠키가 누락되었습니다! 프론트엔드 credentials 설정을 확인해주세요. (요청 URI: {})", uri);
            writeError(response, HttpStatus.BAD_REQUEST,
                    "대기열 토큰이 필요합니다. 대기열을 통해 입장해주세요.",
                    "QUEUE_TOKEN_MISSING");
            return;
        }

        // 2. Redis에서 토큰 존재 여부만 확인 (가볍게!)
        String tokenKey = TOKEN_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(tokenKey);

        if (exists == null || !exists) {
            log.warn("🚨 [QueueTokenFilter] API 차단 - 유효하지 않거나 만료된 대기열 토큰입니다. (전달받은 토큰: {})", token);
            writeError(response, HttpStatus.BAD_REQUEST,
                    "유효하지 않은 대기열 토큰입니다. 토큰이 만료되었거나 존재하지 않습니다.",
                    "QUEUE_TOKEN_INVALID");
            return;
        }

        // 3. 토큰이 Redis에 존재 → 통과 (세부 검증은 ReservationFacade에서)
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message, String code)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message, code, null)));
    }

    /**
     * 요청에서 QUEUE-TOKEN 쿠키 값을 추출
     */
    private String extractQueueTokenFromCookie(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (jakarta.servlet.http.Cookie cookie : cookies) {
            if (QUEUE_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
