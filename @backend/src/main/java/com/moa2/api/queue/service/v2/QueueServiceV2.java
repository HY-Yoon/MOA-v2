package com.moa2.api.queue.service.v2;

import com.moa2.api.queue.dto.QueueDtoV2;
import com.moa2.api.show.domain.repository.ShowScheduleRepository;
import com.moa2.global.token.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.context.annotation.Profile;

/**
 * V2: Redis 기반 대기열 서비스
 * - Redis Sorted Set(ZSet)으로 대기열 관리
 * - 순서 보장, 빠른 순위 조회, 메모리 기반 처리
 */
@Slf4j
@Profile("v2")
@Service
@RequiredArgsConstructor
public class QueueServiceV2 {

    private final RedisTemplate<String, String> redisTemplate;
    private final TokenService tokenService;
    private final ShowScheduleRepository showScheduleRepository;

    @Value("${queue.scheduler.batch-size:100}")
    private int batchSize;

    @Value("${queue.scheduler.interval-ms:1000}")
    private int intervalMs;

    private static final String QUEUE_KEY_PREFIX = "waiting:queue:";
    private static final String ACTIVE_SCHEDULES_KEY = "active:schedules";

    /**
     * 대기열 진입
     * - Redis ZSet에 사용자 추가 (score: 현재 시간)
     * - 내 앞에 아무도 없으면 즉시 토큰 발급
     *
     * @param userId     사용자 ID
     * @param scheduleId 스케줄 ID
     * @return 대기열 등록 결과
     */
    public QueueDtoV2.EnterResponse enterQueue(Long userId, Long scheduleId) {
        log.info("V2 대기열 진입 요청 - userId: {}, scheduleId: {}", userId, scheduleId);

        // 1. 스케줄 존재 확인
        if (!showScheduleRepository.existsById(scheduleId)) {
            throw new IllegalArgumentException("존재하지 않는 스케줄입니다");
        }

        // 2. 이미 토큰이 있는지 확인 (새로고침 대응)
        String existingToken = tokenService.findTokenByUser(userId, scheduleId);
        if (existingToken != null) {
            log.info("이미 토큰 보유 중 - userId: {}, token: {}", userId, existingToken);
            return QueueDtoV2.EnterResponse.ready(existingToken);
        }

        String queueKey = QUEUE_KEY_PREFIX + scheduleId;
        String userIdStr = userId.toString();

        // 3. 이미 대기열에 있는지 확인
        Double existingScore = redisTemplate.opsForZSet().score(queueKey, userIdStr);
        if (existingScore != null) {
            // 이미 대기 중 - 현재 위치 반환
            Long rank = redisTemplate.opsForZSet().rank(queueKey, userIdStr);
            long position = (rank != null) ? rank + 1 : 1;
            long estimatedWait = calculateWaitTimeSeconds(position);

            log.info("이미 대기열에 등록됨 - userId: {}, position: {}", userId, position);
            return QueueDtoV2.EnterResponse.alreadyWaiting(position, estimatedWait);
        }

        // 4. 대기열에 추가
        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(queueKey, userIdStr, score);

        // 5. 활성 스케줄 목록에 추가 (스케줄러가 처리할 대상)
        redisTemplate.opsForSet().add(ACTIVE_SCHEDULES_KEY, scheduleId.toString());

        // 6. 내 순위 확인
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userIdStr);
        long position = (rank != null) ? rank + 1 : 1;

        // 7. 첫 번째 사람이면 즉시 토큰 발급
        if (position == 1) {
            String token = tokenService.issueToken(userId, scheduleId);

            // 대기열에서 제거
            redisTemplate.opsForZSet().remove(queueKey, userIdStr);

            log.info("즉시 토큰 발급 (첫 번째) - userId: {}, token: {}", userId, token);
            return QueueDtoV2.EnterResponse.ready(token);
        }

        long estimatedWait = calculateWaitTimeSeconds(position);
        log.info("대기열 등록 완료 - userId: {}, position: {}, estimatedWait: {}초",
                userId, position, estimatedWait);

        return QueueDtoV2.EnterResponse.waiting(position, estimatedWait);
    }

    /**
     * 대기열 상태 조회
     * - 먼저 토큰 확인 (새로고침 대응)
     * - 토큰 없으면 대기열에서 순위 확인
     *
     * @param userId     사용자 ID
     * @param scheduleId 스케줄 ID
     * @return 대기열 상태
     */
    public QueueDtoV2.StatusResponse getQueueStatus(Long userId, Long scheduleId) {
        log.debug("V2 대기열 상태 조회 - userId: {}, scheduleId: {}", userId, scheduleId);

        // 1. 먼저 토큰 확인 (새로고침해도 토큰 유지)
        String existingToken = tokenService.findTokenByUser(userId, scheduleId);
        if (existingToken != null) {
            log.debug("토큰 보유 중 - READY 상태: userId={}", userId);
            return QueueDtoV2.StatusResponse.ready(existingToken);
        }

        // 2. 대기열에서 순위 확인
        String queueKey = QUEUE_KEY_PREFIX + scheduleId;
        String userIdStr = userId.toString();

        Long rank = redisTemplate.opsForZSet().rank(queueKey, userIdStr);

        if (rank == null) {
            // 대기열에도 없고 토큰도 없음
            log.debug("대기열에 없음 - NOT_FOUND: userId={}", userId);
            return QueueDtoV2.StatusResponse.notFound();
        }

        // 3. WAITING 상태
        long position = rank + 1;
        long estimatedWait = calculateWaitTimeSeconds(position);

        log.debug("대기 중 - position: {}, estimatedWait: {}초", position, estimatedWait);
        return QueueDtoV2.StatusResponse.waiting(position, estimatedWait);
    }

    /**
     * 대기열 이탈 (선택적)
     *
     * @param userId     사용자 ID
     * @param scheduleId 스케줄 ID
     */
    public void exitQueue(Long userId, Long scheduleId) {
        String queueKey = QUEUE_KEY_PREFIX + scheduleId;
        redisTemplate.opsForZSet().remove(queueKey, userId.toString());
        log.info("대기열 이탈 - userId: {}, scheduleId: {}", userId, scheduleId);
    }

    /**
     * 대기 시간 계산 (올림 처리)
     * - position / batchSize × intervalMs / 1000
     * - 최소 1초 보장
     *
     * @param position 내 앞 대기 인원 수 (1-based)
     * @return 예상 대기 시간 (초)
     */
    public long calculateWaitTimeSeconds(long position) {
        // position이 99일 때: ceil(99 / 100.0) = 1
        long cycles = (long) Math.ceil((double) position / batchSize);
        long waitTimeMs = cycles * intervalMs;
        long waitTimeSeconds = waitTimeMs / 1000;

        // 최소 1초 보장
        return Math.max(waitTimeSeconds, 1);
    }

    /**
     * 대기열 키 생성 (외부 접근용)
     */
    public String getQueueKey(Long scheduleId) {
        return QUEUE_KEY_PREFIX + scheduleId;
    }

    /**
     * 활성 스케줄 키 (외부 접근용)
     */
    public String getActiveSchedulesKey() {
        return ACTIVE_SCHEDULES_KEY;
    }
}
