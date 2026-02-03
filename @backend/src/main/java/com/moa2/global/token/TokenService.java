package com.moa2.global.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * V2: 입장 토큰 관리 서비스
 * - 대기열 → 예매 진행 시 필요한 토큰 발급/검증
 * - Queue와 Reservation 모두에서 사용 (공통 인프라)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${queue.token.ttl-minutes:5}")
    private int tokenTtlMinutes;

    private static final String TOKEN_PREFIX = "token:";
    private static final String USER_TOKEN_PREFIX = "user:token:";

    /**
     * 토큰 발급
     * - 스케줄러가 대기열 통과자에게 발급
     *
     * @param userId     사용자 ID
     * @param scheduleId 스케줄 ID
     * @return 발급된 토큰 (UUID)
     */
    public String issueToken(Long userId, Long scheduleId) {
        String token = UUID.randomUUID().toString();
        String tokenKey = TOKEN_PREFIX + token;

        TokenInfo info = TokenInfo.builder()
                .userId(userId)
                .scheduleId(scheduleId)
                .used(false)
                .build();

        try {
            String infoJson = objectMapper.writeValueAsString(info);

            // 토큰 저장 (TTL 적용)
            redisTemplate.opsForValue().set(tokenKey, infoJson, tokenTtlMinutes, TimeUnit.MINUTES);

            // 역방향 조회용 (userId+scheduleId -> token)
            // 새로고침 시 기존 토큰 찾기 위함
            String userTokenKey = USER_TOKEN_PREFIX + userId + ":" + scheduleId;
            redisTemplate.opsForValue().set(userTokenKey, token, tokenTtlMinutes, TimeUnit.MINUTES);

            log.info("토큰 발급 완료: userId={}, scheduleId={}, token={}", userId, scheduleId, token);
            return token;

        } catch (JsonProcessingException e) {
            log.error("토큰 발급 중 JSON 변환 오류", e);
            throw new RuntimeException("토큰 발급 실패", e);
        }
    }

    /**
     * 토큰 검증
     * - 예매 시 토큰 유효성 확인
     *
     * @param token 토큰
     * @return 토큰 정보
     * @throws IllegalArgumentException 토큰이 유효하지 않은 경우
     */
    public TokenInfo validateToken(String token) {
        String tokenKey = TOKEN_PREFIX + token;
        String infoJson = redisTemplate.opsForValue().get(tokenKey);

        if (infoJson == null) {
            log.warn("토큰 검증 실패 - 만료되었거나 존재하지 않음: token={}", token);
            throw new IllegalArgumentException("토큰이 만료되었거나 유효하지 않습니다");
        }

        try {
            TokenInfo info = objectMapper.readValue(infoJson, TokenInfo.class);

            if (info.isUsed()) {
                log.warn("토큰 검증 실패 - 이미 사용된 토큰: token={}", token);
                throw new IllegalArgumentException("이미 사용된 토큰입니다");
            }

            log.debug("토큰 검증 성공: token={}, userId={}", token, info.getUserId());
            return info;

        } catch (JsonProcessingException e) {
            log.error("토큰 검증 중 JSON 파싱 오류", e);
            throw new RuntimeException("토큰 검증 실패", e);
        }
    }

    /**
     * 토큰 사용 처리 (재사용 방지)
     * - 예매 완료 후 호출
     *
     * @param token 토큰
     */
    public void consumeToken(String token) {
        String tokenKey = TOKEN_PREFIX + token;
        String infoJson = redisTemplate.opsForValue().get(tokenKey);

        if (infoJson == null) {
            log.warn("토큰 소진 실패 - 이미 만료됨: token={}", token);
            return;
        }

        try {
            TokenInfo info = objectMapper.readValue(infoJson, TokenInfo.class);
            info.setUsed(true);

            // 남은 TTL 유지하면서 업데이트
            Long ttl = redisTemplate.getExpire(tokenKey, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                String updatedJson = objectMapper.writeValueAsString(info);
                redisTemplate.opsForValue().set(tokenKey, updatedJson, ttl, TimeUnit.SECONDS);
            }

            log.info("토큰 소진 완료: token={}", token);

        } catch (JsonProcessingException e) {
            log.error("토큰 소진 중 JSON 변환 오류", e);
        }
    }

    /**
     * 유저의 기존 토큰 조회
     * - 새로고침 시 대참사 방지용
     * - 이미 토큰이 발급되었다면 대기열에 다시 줄 서지 않도록
     *
     * @param userId     사용자 ID
     * @param scheduleId 스케줄 ID
     * @return 기존 토큰 (없으면 null)
     */
    public String findTokenByUser(Long userId, Long scheduleId) {
        String userTokenKey = USER_TOKEN_PREFIX + userId + ":" + scheduleId;
        String token = redisTemplate.opsForValue().get(userTokenKey);

        if (token != null) {
            // 토큰이 아직 유효한지 확인
            String tokenKey = TOKEN_PREFIX + token;
            String infoJson = redisTemplate.opsForValue().get(tokenKey);

            if (infoJson != null) {
                try {
                    TokenInfo info = objectMapper.readValue(infoJson, TokenInfo.class);
                    if (!info.isUsed()) {
                        log.debug("기존 토큰 발견: userId={}, scheduleId={}, token={}", userId, scheduleId, token);
                        return token;
                    }
                } catch (JsonProcessingException e) {
                    log.error("토큰 조회 중 JSON 파싱 오류", e);
                }
            }
        }

        return null;
    }

    /**
     * 토큰 삭제 (명시적 삭제가 필요한 경우)
     *
     * @param token 토큰
     */
    public void deleteToken(String token) {
        String tokenKey = TOKEN_PREFIX + token;
        redisTemplate.delete(tokenKey);
        log.debug("토큰 삭제 완료: token={}", token);
    }
}
