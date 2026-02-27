package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.dto.ReservationDtoV2;
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
import java.util.concurrent.TimeUnit;

/**
 * V2: Redisson 분산 락 기반 예매 서비스
 *
 * [트랜잭션 분리 설계]
 * reserve()              : 트랜잭션 없음 - Redis 분산 락 획득/해제만 담당
 * ReservationPersistService : @Transactional - DB 쓰기(예약, 좌석 상태, 결제)만 담당
 *
 * 이렇게 분리하는 이유:
 * - reserve()에 @Transactional이 걸려 있으면, 락 대기(최대 3초) 동안
 *   DB 커넥션을 아무 일 없이 물고 있게 됨
 * - 동시 요청 수백 건이 몰리면 HikariCP 커넥션 풀이 고갈 → 500 에러 폭발
 * - DB 커넥션은 "진짜 DB에 쓸 때만" 짧게 사용하도록 별도 클래스로 분리
 */
@Slf4j
@Profile("v2")
@Service
@RequiredArgsConstructor
public class ReservationServiceV2 {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final ReservationPersistService reservationPersistService;

    @Value("${queue.token.ttl-minutes:5}")
    private int lockTtlMinutes;

    private static final String SEAT_LOCK_PREFIX = "seat:lock:";
    private static final String SEAT_STATUS_PREFIX = "seat:status:";

    /**
     * 예약 처리 (분산 락 적용, 트랜잭션 없음)
     * - Redis 분산 락으로 좌석 동시성 제어
     * - DB 커넥션은 saveReservationData()에서만 사용
     */
    public ReservationDtoV2.ReserveResponse reserve(
            Long userId,
            Long scheduleId,
            List<Long> seatIds) {
        log.info("V2 예매 시작 - userId: {}, scheduleId: {}, seatIds: {}", userId, scheduleId, seatIds);

        // 1. 좌석 ID 정렬 (데드락 방지)
        List<Long> sortedSeatIds = new ArrayList<>(seatIds);
        sortedSeatIds.sort(Long::compareTo);

        List<ScheduleSeat> reservedSeats = new ArrayList<>();
        List<RLock> acquiredLocks = new ArrayList<>();
        List<String> conflictSeatNumbers = new ArrayList<>();
        int totalAmount = 0;

        try {
            // 2. 각 좌석에 대해 Redis 분산 락 획득 및 검증
            for (Long seatId : sortedSeatIds) {
                RLock lock = redissonClient.getLock(SEAT_LOCK_PREFIX + seatId);

                try {
                    // 3초 대기, 5초 후 자동 해제 (기존 10초 → 3초로 단축)
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

                    if ("RESERVED".equals(cachedStatus) || "SOLD".equals(cachedStatus)) {
                        conflictSeatNumbers.add(String.valueOf(seatId));
                        continue;
                    }

                    // Redis 캐시 업데이트 (선점 표시)
                    redisTemplate.opsForValue().set(statusKey, "RESERVED", lockTtlMinutes, TimeUnit.MINUTES);

                    // 좌석 엔티티 조회 (트랜잭션 밖이므로 detached 상태)
                    ScheduleSeat scheduleSeat = scheduleSeatRepository.findById(seatId)
                            .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다: " + seatId));

                    if (scheduleSeat.getStatus() != SeatStatus.AVAILABLE) {
                        conflictSeatNumbers.add(String.valueOf(seatId));
                        // Redis 캐시 롤백
                        redisTemplate.delete(statusKey);
                        continue;
                    }

                    if (!scheduleSeat.getSchedule().getId().equals(scheduleId)) {
                        throw new IllegalArgumentException("해당 스케줄의 좌석이 아닙니다: " + seatId);
                    }

                    reservedSeats.add(scheduleSeat);
                    totalAmount += scheduleSeat.getGrade().getPrice();
                    log.debug("좌석 선점 성공: seatId={}", seatId);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("좌석 선점 중 인터럽트 발생", e);
                }
            }

            // 충돌된 좌석이 있으면 SeatConflictException 발생
            if (!conflictSeatNumbers.isEmpty()) {
                throw new SeatConflictException(conflictSeatNumbers);
            }

            // 3. DB 저장 (여기서만 트랜잭션 + DB 커넥션 사용!)
            return reservationPersistService.saveReservationData(userId, scheduleId, reservedSeats, totalAmount, lockTtlMinutes);

        } finally {
            // 4. 락 해제 (항상 실행)
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
