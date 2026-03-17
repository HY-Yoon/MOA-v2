package com.moa2.api.queue.scheduler;

import com.moa2.global.token.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

import org.springframework.context.annotation.Profile;

/**
 * V2: 대기열 처리 스케줄러
 * - 주기적으로 대기열에서 상위 N명을 꺼내 토큰 발급
 * - ShedLock으로 다중 서버 환경에서 중복 실행 방지
 * - Redis Pipeline으로 네트워크 왕복 최소화 (리전 무관 영구 최적화)
 */
@Slf4j
@Profile("v2")
@Component
@RequiredArgsConstructor
public class QueueProcessorScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final TokenService tokenService;

    @Value("${queue.scheduler.batch-size:30}")
    private int batchSize;

    private static final String QUEUE_KEY_PREFIX = "waiting:queue:";
    private static final String ACTIVE_SCHEDULES_KEY = "active:schedules";

    /**
     * 대기열 처리
     * - 설정된 간격(기본 1초)마다 실행
     * - ShedLock으로 단일 서버만 실행 보장
     */
    @Scheduled(fixedDelayString = "${queue.scheduler.interval-ms:1000}")
    @SchedulerLock(name = "QueueProcessorScheduler_processQueue", lockAtMostFor = "${queue.scheduler.lock-at-most-for:5s}", lockAtLeastFor = "500ms")
    public void processQueue() {
        try {
            // 활성 스케줄 목록 조회
            Set<String> activeScheduleIds = redisTemplate.opsForSet().members(ACTIVE_SCHEDULES_KEY);

            if (activeScheduleIds == null || activeScheduleIds.isEmpty()) {
                return;
            }

            for (String scheduleIdStr : activeScheduleIds) {
                try {
                    Long scheduleId = Long.parseLong(scheduleIdStr);
                    processScheduleQueue(scheduleId);
                } catch (NumberFormatException e) {
                    log.error("스케줄 ID 파싱 오류: {}", scheduleIdStr, e);
                }
            }
        } catch (Exception e) {
            log.error("대기열 처리 중 오류 발생", e);
        }
    }

    /**
     * 특정 스케줄의 대기열 처리 (Pipeline 최적화)
     *
     * [기존] 유저당 Redis 3~5회 직렬 호출 → N명이면 N×5회 왕복
     * [개선] Pipeline 2~3회 왕복으로 N명 전체 처리
     *
     * 1차: Pipeline MGET - 모든 유저의 기존 토큰 일괄 조회
     * 2차: Pipeline SET + ZREM - 새 토큰 발급 + 대기열 제거 일괄 실행
     */
    private void processScheduleQueue(Long scheduleId) {
        String queueKey = QUEUE_KEY_PREFIX + scheduleId;

        // 상위 batchSize명 조회
        Set<String> userIds = redisTemplate.opsForZSet().range(queueKey, 0, batchSize - 1);

        if (userIds == null || userIds.isEmpty()) {
            // 대기열이 비었으면 활성 스케줄 목록에서 제거
            redisTemplate.opsForSet().remove(ACTIVE_SCHEDULES_KEY, scheduleId.toString());
            return;
        }

        List<String> userIdList = new ArrayList<>(userIds);

        // ============================================================
        // [1차 Pipeline] 모든 유저의 기존 토큰 일괄 조회 — Redis 왕복 1회
        // ============================================================
        List<long[]> allPairs = new ArrayList<>();
        for (String userIdStr : userIdList) {
            allPairs.add(new long[]{Long.parseLong(userIdStr), scheduleId});
        }

        Map<Long, String> existingTokens = tokenService.findTokensByUserBatch(allPairs);

        // 이미 토큰이 있는 유저와 없는 유저 분리
        List<String> alreadyHaveToken = new ArrayList<>();
        List<long[]> needToken = new ArrayList<>();

        for (String userIdStr : userIdList) {
            Long userId = Long.parseLong(userIdStr);
            if (existingTokens.containsKey(userId)) {
                alreadyHaveToken.add(userIdStr);
            } else {
                needToken.add(new long[]{userId, scheduleId});
            }
        }

        // ============================================================
        // [2차 Pipeline] 새 토큰 일괄 발급 — Redis 왕복 1회
        // ============================================================
        Map<Long, String> issuedTokens = tokenService.issueTokenBatch(needToken);

        // ============================================================
        // [3차 Pipeline] 대기열에서 일괄 제거 — Redis 왕복 1회
        // ============================================================
        List<String> allToRemove = new ArrayList<>();
        allToRemove.addAll(alreadyHaveToken);
        for (long[] pair : needToken) {
            allToRemove.add(String.valueOf(pair[0]));
        }

        if (!allToRemove.isEmpty()) {
            redisTemplate.opsForZSet().remove(queueKey, allToRemove.toArray());
        }

        if (!issuedTokens.isEmpty()) {
            log.info("토큰 발급 완료: scheduleId={}, newCount={}, skipped={}",
                    scheduleId, issuedTokens.size(), alreadyHaveToken.size());
        }

        // 대기열이 완전히 비었는지 확인
        Long remainingCount = redisTemplate.opsForZSet().zCard(queueKey);
        if (remainingCount == null || remainingCount == 0) {
            redisTemplate.opsForSet().remove(ACTIVE_SCHEDULES_KEY, scheduleId.toString());
            log.info("대기열 처리 완료 - 활성 목록에서 제거: scheduleId={}", scheduleId);
        }
    }
}
