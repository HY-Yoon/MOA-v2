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
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.lang.NonNull;

/**
 * V2: 좌석 조회 전 Redis 토큰 보유 여부 검증 인터셉터
 * - /api/v1/schedules/{scheduleId}/seats 경로 보호
 * - API 요청 시 QUEUE-TOKEN 쿠키의 토큰이 Redis에 존재하는지만 확인 (DB 접근 없음)
 * - QueueReadyInterceptor(DB 기반, V1용)를 V2에서 대체
 */
@Slf4j
@Profile("v2")
@Component
@RequiredArgsConstructor
public class QueueTokenInterceptor implements HandlerInterceptor {

    private static final Pattern SCHEDULE_SEATS_URI_PATTERN = Pattern.compile("^/api/v1/schedules/(\\d+)/seats$");

    private static final String QUEUE_TOKEN_COOKIE = "QUEUE-TOKEN";
    private static final String TOKEN_PREFIX = "token:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler)
            throws Exception {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // CORS preflight(OPTIONS)는 토큰 검증 없이 통과
        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }

        // /api/v1/schedules/{id}/seats 경로만 적용
        Matcher matcher = SCHEDULE_SEATS_URI_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            return true;
        }

        // QUEUE-TOKEN 쿠키 확인
        String token = extractQueueTokenFromCookie(request);
        if (token == null || token.isBlank()) {
            log.warn("🚨 [QueueTokenInterceptor] V2 좌석 조회 차단 - 'QUEUE-TOKEN' 쿠키가 누락되었습니다! (요청 URI: {})", uri);
            writeError(response, HttpStatus.FORBIDDEN,
                    "대기열을 통과한 사용자만 접근할 수 있습니다. 먼저 대기열에 진입해주세요.",
                    "QUEUE_TOKEN_MISSING");
            return false;
        }

        // Redis에서 토큰 정보 조회
        String tokenKey = TOKEN_PREFIX + token;
        String tokenJson = redisTemplate.opsForValue().get(tokenKey);
        
        if (tokenJson == null) {
            log.warn("🚨 [QueueTokenInterceptor] V2 좌석 조회 차단 - 유효하지 않거나 만료된 대기열 토큰입니다. (전달받은 토큰: {})", token);
            writeError(response, HttpStatus.FORBIDDEN,
                    "대기열 토큰이 만료되었거나 유효하지 않습니다. 다시 대기열에 진입해주세요.",
                    "QUEUE_TOKEN_INVALID");
            return false;
        }

        // ⭐️ 핵심 보안 로직: 발급받은 토큰의 scheduleId와 현재 접근하려는 URL의 scheduleId 검증
        try {
            com.moa2.global.token.TokenInfo info = objectMapper.readValue(tokenJson, com.moa2.global.token.TokenInfo.class);
            Long requestScheduleId = Long.parseLong(matcher.group(1));

            if (!requestScheduleId.equals(info.getScheduleId())) {
                log.warn("V2 좌석 조회 보안 차단 - 회차 우회 시도 발생! 요청 URI: {}, 토큰 소유자 userID: {}, 토큰 발급 회차: {}", 
                         uri, info.getUserId(), info.getScheduleId());
                
                // 프론트에서 안전하게 사용할 수 있도록 targetScheduleId를 data 영역에 담아서 보냅니다.
                java.util.Map<String, Object> errorData = new java.util.HashMap<>();
                errorData.put("targetScheduleId", requestScheduleId);
                
                writeErrorWithData(response, HttpStatus.BAD_REQUEST,
                        "해당 회차에 대한 접근 권한이 없습니다.",
                        "INVALID_QUEUE_TOKEN",
                        errorData);
                return false;
            }

            if (info.isUsed()) {
                log.warn("V2 좌석 조회 차단 - 이미 사용된 토큰: {}", token);
                writeError(response, HttpStatus.FORBIDDEN,
                        "이미 결제가 진행되었거나 사용 만료된 토큰입니다.",
                        "QUEUE_TOKEN_USED");
                return false;
            }
            
        } catch (Exception e) {
            log.error("V2 토큰 JSON 파싱 오류: {}", e.getMessage());
            writeError(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "토큰 검증 중 서버 오류가 발생했습니다.",
                    "QUEUE_TOKEN_PARSE_ERROR");
            return false;
        }

        log.debug("V2 좌석 조회 허용 - 토큰 유효: {}", token);
        return true;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message, String code)
            throws Exception {
        writeErrorWithData(response, status, message, code, null);
    }
    
    private void writeErrorWithData(HttpServletResponse response, HttpStatus status, String message, String code, Object data)
            throws Exception {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message, code, data)));
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
