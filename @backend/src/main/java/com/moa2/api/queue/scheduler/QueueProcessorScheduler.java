package com.moa2.api.queue.scheduler;

import com.moa2.global.token.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * V2: 대기열 처리 스케줄러
 * - 주기적으로 대기열에서 상위 N명을 꺼내 토큰 발급
 * - ShedLock으로 다중 서버 환경에서 중복 실행 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueProcessorScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final TokenService tokenService;

    @Value("${queue.scheduler.batch-size:100}")
    private int batchSize;

    private static final String QUEUE_KEY_PREFIX = "waiting:queue:";
    private static final String ACTIVE_SCHEDULES_KEY = "active:schedules";

    /**
     * 대기열 처리
     * - 1초마다 실행 (interval-ms 설정)
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
     * 특정 스케줄의 대기열 처리
     * - 상위 batchSize명에게 토큰 발급
     * - 대기열에서 제거
     */
    private void processScheduleQueue(Long scheduleId) {
        String queueKey = QUEUE_KEY_PREFIX + scheduleId;

        // 상위 batchSize명 조회 (0 ~ batchSize - 1)
        Set<String> userIds = redisTemplate.opsForZSet().range(queueKey, 0, batchSize - 1);

        if (userIds == null || userIds.isEmpty()) {
            // 대기열이 비었으면 활성 스케줄 목록에서 제거
            redisTemplate.opsForSet().remove(ACTIVE_SCHEDULES_KEY, scheduleId.toString());
            return;
        }

        int issuedCount = 0;
        for (String userIdStr : userIds) {
            try {
                Long userId = Long.parseLong(userIdStr);

                // 이미 토큰이 있는지 확인 (중복 발급 방지)
                String existingToken = tokenService.findTokenByUser(userId, scheduleId);
                if (existingToken != null) {
                    // 이미 토큰 있음 - 대기열에서만 제거
                    redisTemplate.opsForZSet().remove(queueKey, userIdStr);
                    log.debug("이미 토큰 보유 - 대기열 제거: userId={}", userId);
                    continue;
                }

                // 토큰 발급
                tokenService.issueToken(userId, scheduleId);

                // 대기열에서 제거
                redisTemplate.opsForZSet().remove(queueKey, userIdStr);

                issuedCount++;

            } catch (Exception e) {
                log.error("토큰 발급 실패: userId={}, scheduleId={}", userIdStr, scheduleId, e);
            }
        }

        if (issuedCount > 0) {
            log.info("토큰 발급 완료: scheduleId={}, count={}", scheduleId, issuedCount);
        }

        // 대기열이 완전히 비었는지 확인
        Long remainingCount = redisTemplate.opsForZSet().zCard(queueKey);
        if (remainingCount == null || remainingCount == 0) {
            redisTemplate.opsForSet().remove(ACTIVE_SCHEDULES_KEY, scheduleId.toString());
            log.info("대기열 처리 완료 - 활성 목록에서 제거: scheduleId={}", scheduleId);
        }
    }
}
