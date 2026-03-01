package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.dto.ReservationDtoV2;
import com.moa2.api.reservation.dto.ReservationRequestEvent;
import com.moa2.api.reservation.exception.SeatConflictException;
import com.moa2.api.show.domain.entity.ScheduleSeat;
import com.moa2.api.show.domain.repository.ScheduleSeatRepository;
import com.moa2.global.model.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * V2: Redisson 분산 락 기반 예매 서비스 (Kafka 비동기 처리)
 *
 * [Kafka 도입 후 흐름]
 * 1. Redis 분산 락 획득 + 좌석 유효성 검증
 * 2. Kafka에 예매 이벤트 발행 (DB 커넥션 점유 없음)
 * 3. 즉시 202 Accepted 응답
 * 4. Consumer가 비동기로 DB 저장 (ReservationPersistService)
 */
@Slf4j
@Profile("v2")
@Service
@RequiredArgsConstructor
public class ReservationServiceV2 {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final ReservationKafkaProducer kafkaProducer;

    @Value("${queue.token.ttl-minutes:5}")
    private int lockTtlMinutes;

    private static final String SEAT_LOCK_PREFIX = "seat:lock:";
    private static final String SEAT_STATUS_PREFIX = "seat:status:";

    /**
     * 예약 처리 (분산 락 + Kafka 비동기 발행)
     * - Redis 분산 락으로 좌석 동시성 제어
     * - Kafka에 이벤트 발행 후 즉시 응답 (DB 커넥션 사용 없음)
     */
    public ReservationDtoV2.ReserveAcceptedResponse reserve(
            Long userId,
            Long scheduleId,
            List<Long> seatIds) {
        log.info("V2 예매 시작 - userId: {}, scheduleId: {}, seatIds: {}", userId, scheduleId, seatIds);

        // 1. 좌석 ID 정렬 (데드락 방지)
        List<Long> sortedSeatIds = new ArrayList<>(seatIds);
        sortedSeatIds.sort(Long::compareTo);

        List<Long> validatedSeatIds = new ArrayList<>();
        List<RLock> acquiredLocks = new ArrayList<>();
        List<String> conflictSeatNumbers = new ArrayList<>();
        int totalAmount = 0;

        String eventId = UUID.randomUUID().toString();

        try {
            // 2. 각 좌석에 대해 Redis 분산 락 획득 및 검증
            for (Long seatId : sortedSeatIds) {
                RLock lock = redissonClient.getLock(SEAT_LOCK_PREFIX + seatId);

                try {
                    // 3초 대기, 5초 후 자동 해제
                    boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);

                    if (!acquired) {
                        log.warn("좌석 락 획득 실패: seatId={}", seatId);
                        conflictSeatNumbers.add(String.valueOf(seatId));
                        continue;
                    }

                    acquiredLocks.add(lock);

                    // Redis 캐시 확인 (빠른 중복 체크 - DB 접근 없이)
                    String statusKey = SEAT_STATUS_PREFIX + seatId;
                    String cachedStatus = redisTemplate.opsForValue().get(statusKey);

                    if ("RESERVED".equals(cachedStatus) || "SOLD".equals(cachedStatus)
                            || "PENDING_KAFKA".equals(cachedStatus) || "CONFIRMED".equals(cachedStatus)) {
                        conflictSeatNumbers.add(String.valueOf(seatId));
                        continue;
                    }

                    // 좌석 엔티티 조회 (유효성 검증용)
                    ScheduleSeat scheduleSeat = scheduleSeatRepository.findById(seatId)
                            .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다: " + seatId));

                    if (scheduleSeat.getStatus() != SeatStatus.AVAILABLE) {
                        conflictSeatNumbers.add(String.valueOf(seatId));
                        continue;
                    }

                    if (!scheduleSeat.getSchedule().getId().equals(scheduleId)) {
                        throw new IllegalArgumentException("해당 스케줄의 좌석이 아닙니다: " + seatId);
                    }

                    // Redis 캐시: PENDING_KAFKA 상태로 설정 (Consumer 처리 전까지 유지)
                    redisTemplate.opsForValue().set(statusKey, "PENDING_KAFKA", 10, TimeUnit.MINUTES);

                    validatedSeatIds.add(seatId);
                    totalAmount += scheduleSeat.getGrade().getPrice();
                    log.debug("좌석 검증 성공: seatId={}", seatId);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("좌석 선점 중 인터럽트 발생", e);
                }
            }

            // 충돌된 좌석이 있으면 SeatConflictException 발생
            if (!conflictSeatNumbers.isEmpty()) {
                throw new SeatConflictException(conflictSeatNumbers);
            }

            // 3. Kafka 이벤트 발행 (DB 커넥션 사용 없이 비동기 처리!)
            ReservationRequestEvent event = ReservationRequestEvent.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .scheduleId(scheduleId)
                    .scheduleSeatIds(validatedSeatIds)
                    .totalAmount(totalAmount)
                    .lockTtlMinutes(lockTtlMinutes)
                    .build();

            try {
                kafkaProducer.send(event);
            } catch (Exception e) {
                // ★ Producer 실패 시 보상 트랜잭션: Redis 상태 롤백
                log.error("Kafka 이벤트 발행 실패 - 보상 트랜잭션 실행", e);
                for (Long seatId : validatedSeatIds) {
                    redisTemplate.delete(SEAT_STATUS_PREFIX + seatId);
                }
                throw new RuntimeException("예매 처리 중 오류가 발생했습니다. 다시 시도해주세요.", e);
            }

            // 4. 예매 상태: PENDING (폴링용)
            redisTemplate.opsForValue().set(
                    "reservation:status:" + eventId, "PENDING", 30, TimeUnit.MINUTES);

            log.info("V2 Kafka 예매 이벤트 발행 완료 - eventId: {}", eventId);

            // 5. 202 Accepted 응답 (DB 저장은 Consumer가 비동기 처리)
            return ReservationDtoV2.ReserveAcceptedResponse.of(eventId);

        } finally {
            // 6. 락 해제 (항상 실행)
            for (RLock lock : acquiredLocks) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (Exception e) {
                    log.error("락 해제 실패", e);
                }
            }
        }
    }
}
