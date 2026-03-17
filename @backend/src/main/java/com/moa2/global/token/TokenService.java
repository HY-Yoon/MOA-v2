package com.moa2.global.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Profile;

/**
 * V2: 입장 토큰 관리 서비스
 * - 대기열 → 예매 진행 시 필요한 토큰 발급/검증
 * - Queue와 Reservation 모두에서 사용 (공통 인프라)
 */
@Slf4j
@Profile("v2")
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

    // ====================================================================
    // Pipeline 기반 배치 메서드 (QueueProcessorScheduler에서 사용)
    // ====================================================================

    /**
     * [Pipeline] 여러 유저의 기존 토큰을 일괄 조회
     * - N명에 대해 Redis 왕복 1회로 처리 (개별 호출 시 N×2회 왕복)
     *
     * @param userSchedulePairs (userId, scheduleId) 쌍 리스트
     * @return userId → 토큰문자열 Map (토큰 없는 유저는 포함되지 않음)
     */
    public Map<Long, String> findTokensByUserBatch(List<long[]> userSchedulePairs) {
        if (userSchedulePairs.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1) user:token:{userId}:{scheduleId} 키 목록 생성
        List<String> userTokenKeys = new ArrayList<>(userSchedulePairs.size());
        for (long[] pair : userSchedulePairs) {
            userTokenKeys.add(USER_TOKEN_PREFIX + pair[0] + ":" + pair[1]);
        }

        // 2) Pipeline MGET - 왕복 1회
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : userTokenKeys) {
                connection.stringCommands().get(key.getBytes());
            }
            return null;
        });

        // 3) 결과 파싱: 토큰이 존재하는 유저만 Map에 넣기
        Map<Long, String> tokenMap = new HashMap<>();
        for (int i = 0; i < userSchedulePairs.size(); i++) {
            Object result = results.get(i);
            if (result != null) {
                String token = result.toString();
                // 토큰 키가 실제로 유효한지는 확인하지 않음 (TTL이 관리)
                tokenMap.put(userSchedulePairs.get(i)[0], token);
            }
        }

        return tokenMap;
    }

    /**
     * [Pipeline] 여러 유저에게 토큰을 일괄 발급
     * - N명에 대해 Redis 왕복 1회로 처리 (개별 호출 시 N×2회 왕복)
     *
     * @param userSchedulePairs (userId, scheduleId) 쌍 리스트
     * @return userId → 발급된 토큰 Map
     */
    public Map<Long, String> issueTokenBatch(List<long[]> userSchedulePairs) {
        if (userSchedulePairs.isEmpty()) {
            return Collections.emptyMap();
        }

        long ttlSeconds = (long) tokenTtlMinutes * 60;
        Map<Long, String> issuedTokens = new HashMap<>();

        // 토큰 생성 및 JSON 준비
        List<String[]> keyValuePairs = new ArrayList<>(); // [tokenKey, infoJson, userTokenKey, token]
        for (long[] pair : userSchedulePairs) {
            long userId = pair[0];
            long scheduleId = pair[1];
            String token = UUID.randomUUID().toString();

            TokenInfo info = TokenInfo.builder()
                    .userId(userId)
                    .scheduleId(scheduleId)
                    .used(false)
                    .build();

            try {
                String infoJson = objectMapper.writeValueAsString(info);
                String tokenKey = TOKEN_PREFIX + token;
                String userTokenKey = USER_TOKEN_PREFIX + userId + ":" + scheduleId;

                keyValuePairs.add(new String[]{tokenKey, infoJson, userTokenKey, token});
                issuedTokens.put(userId, token);
            } catch (JsonProcessingException e) {
                log.error("토큰 발급 중 JSON 변환 오류: userId={}", userId, e);
            }
        }

        // Pipeline SET - 왕복 1회로 모든 SET + EXPIRE 실행
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String[] kv : keyValuePairs) {
                // tokenKey → infoJson (TTL)
                connection.stringCommands().setEx(
                        kv[0].getBytes(), ttlSeconds, kv[1].getBytes());
                // userTokenKey → token (TTL)
                connection.stringCommands().setEx(
                        kv[2].getBytes(), ttlSeconds, kv[3].getBytes());
            }
            return null;
        });

        log.info("토큰 일괄 발급 완료: count={}", issuedTokens.size());
        return issuedTokens;
    }
}
