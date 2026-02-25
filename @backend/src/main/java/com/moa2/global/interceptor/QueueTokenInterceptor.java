package com.moa2.global.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa2.global.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.lang.NonNull;

/**
 * V2: 좌석 조회 전 Redis 토큰 보유 여부 검증 인터셉터
 * - /api/v1/schedules/{scheduleId}/seats 경로 보호
 * - X-Queue-Token 헤더의 토큰이 Redis에 존재하는지만 확인 (DB 접근 없음)
 * - QueueReadyInterceptor(DB 기반, V1용)를 V2에서 대체
 */
@Slf4j
@Profile("v2")
@Component
@RequiredArgsConstructor
public class QueueTokenInterceptor implements HandlerInterceptor {

    private static final Pattern SCHEDULE_SEATS_URI_PATTERN = Pattern.compile("^/api/v1/schedules/(\\d+)/seats$");

    private static final String QUEUE_TOKEN_HEADER = "X-Queue-Token";
    private static final String TOKEN_PREFIX = "token:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler)
            throws Exception {

        String uri = request.getRequestURI();

        // /api/v1/schedules/{id}/seats 경로만 적용
        Matcher matcher = SCHEDULE_SEATS_URI_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            return true;
        }

        // X-Queue-Token 헤더 확인
        String token = request.getHeader(QUEUE_TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            log.debug("V2 좌석 조회 차단 - 토큰 없음, URI: {}", uri);
            writeError(response, HttpStatus.FORBIDDEN,
                    "대기열을 통과한 사용자만 접근할 수 있습니다. 먼저 대기열에 진입해주세요.");
            return false;
        }

        // Redis에서 토큰 존재 여부만 확인 (DB 접근 없음!)
        String tokenKey = TOKEN_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(tokenKey);
        if (exists == null || !exists) {
            log.debug("V2 좌석 조회 차단 - 유효하지 않은 토큰: {}", token);
            writeError(response, HttpStatus.FORBIDDEN,
                    "대기열 토큰이 만료되었거나 유효하지 않습니다. 다시 대기열에 진입해주세요.");
            return false;
        }

        log.debug("V2 좌석 조회 허용 - 토큰 유효: {}", token);
        return true;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws Exception {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }
}
