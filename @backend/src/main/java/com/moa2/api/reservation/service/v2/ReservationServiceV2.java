package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.entity.ReservationSeat;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.reservation.domain.repository.ReservationSeatRepository;
import com.moa2.api.reservation.dto.ReservationDtoV2;
import com.moa2.api.show.domain.entity.ScheduleSeat;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.repository.ScheduleSeatRepository;
import com.moa2.api.show.domain.repository.ShowScheduleRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.model.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Profile;

/**
 * V2: Redisson 분산 락 기반 예매 서비스
 * - 좌석 선점 + 예약 생성을 한 트랜잭션에서 처리
 * - Redis 분산 락으로 동시성 제어
 */
@Slf4j
@Profile("v2")
@Service
@RequiredArgsConstructor
public class ReservationServiceV2 {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final ShowScheduleRepository showScheduleRepository;
    private final UserRepository userRepository;

    @Value("${queue.token.ttl-minutes:5}")
    private int lockTtlMinutes;

    private static final String SEAT_LOCK_PREFIX = "seat:lock:";
    private static final String SEAT_STATUS_PREFIX = "seat:status:";

    /**
     * 예약 처리 (분산 락 적용)
     * - scheduleId의 seatIds에 대해 Redisson 락 획득
     * - 좌석 상태 확인 → 선점 → 예약 생성
     *
     * @param userId      사용자 ID
     * @param scheduleId  스케줄 ID
     * @param seatIds     좌석 ID 목록
     * @param bookerName  예매자 이름
     * @param bookerPhone 예매자 연락처
     * @param bookerEmail 예매자 이메일
     * @return 예매 결과
     */
    @Transactional
    public ReservationDtoV2.ReserveResponse reserve(
            Long userId,
            Long scheduleId,
            List<Long> seatIds,
            String bookerName,
            String bookerPhone,
            String bookerEmail) {
        log.info("V2 예매 시작 - userId: {}, scheduleId: {}, seatIds: {}", userId, scheduleId, seatIds);

        // 1. 사용자 및 스케줄 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        ShowSchedule schedule = showScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다"));

        // 2. 좌석 ID 정렬 (데드락 방지)
        List<Long> sortedSeatIds = new ArrayList<>(seatIds);
        sortedSeatIds.sort(Long::compareTo);

        List<ScheduleSeat> reservedSeats = new ArrayList<>();
        List<RLock> acquiredLocks = new ArrayList<>();
        int totalAmount = 0;

        try {
            // 3. 각 좌석에 대해 분산 락 획득 및 선점
            for (Long seatId : sortedSeatIds) {
                RLock lock = redissonClient.getLock(SEAT_LOCK_PREFIX + seatId);

                try {
                    // 10초 대기, 5초 후 자동 해제
                    boolean acquired = lock.tryLock(10, 5, TimeUnit.SECONDS);

                    if (!acquired) {
                        log.warn("좌석 락 획득 실패: seatId={}", seatId);
                        throw new IllegalStateException("좌석 선점 중입니다. 잠시 후 다시 시도해주세요.");
                    }

                    acquiredLocks.add(lock);

                    // Redis 캐시 확인 (빠른 중복 체크)
                    String statusKey = SEAT_STATUS_PREFIX + seatId;
                    String cachedStatus = redisTemplate.opsForValue().get(statusKey);

                    if ("RESERVED".equals(cachedStatus) || "SOLD".equals(cachedStatus)) {
                        throw new IllegalStateException("이미 선점된 좌석입니다: " + seatId);
                    }

                    // DB 확인 (Double Check)
                    ScheduleSeat scheduleSeat = scheduleSeatRepository.findById(seatId)
                            .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다: " + seatId));

                    if (scheduleSeat.getStatus() != SeatStatus.AVAILABLE) {
                        throw new IllegalStateException("이미 선점된 좌석입니다: " + seatId);
                    }

                    // 스케줄 검증
                    if (!scheduleSeat.getSchedule().getId().equals(scheduleId)) {
                        throw new IllegalArgumentException("해당 스케줄의 좌석이 아닙니다: " + seatId);
                    }

                    // Redis 캐시 업데이트
                    redisTemplate.opsForValue().set(statusKey, "RESERVED", lockTtlMinutes, TimeUnit.MINUTES);

                    // 좌석 선점 (DB 상태 변경)
                    LocalDateTime lockedUntil = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                            .plusMinutes(lockTtlMinutes);
                    scheduleSeat.lock(userId, lockedUntil);
                    scheduleSeat.reserve();

                    reservedSeats.add(scheduleSeat);
                    totalAmount += scheduleSeat.getGrade().getPrice();

                    log.debug("좌석 선점 성공: seatId={}", seatId);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("좌석 선점 중 인터럽트 발생", e);
                }
            }

            // 4. 예약 생성
            String reservationNumber = generateReservationNumber();
            LocalDateTime paymentDeadline = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .plusMinutes(lockTtlMinutes);

            Reservation reservation = Reservation.builder()
                    .user(user)
                    .showSchedule(schedule)
                    .reservationNumber(reservationNumber)
                    .totalAmount(totalAmount)
                    .seatCount(reservedSeats.size())
                    .bookerName(bookerName)
                    .bookerPhone(bookerPhone)
                    .bookerEmail(bookerEmail)
                    .build();

            reservationRepository.save(reservation);

            // 5. 예약 좌석 연결
            for (ScheduleSeat scheduleSeat : reservedSeats) {
                ReservationSeat reservationSeat = ReservationSeat.builder()
                        .reservation(reservation)
                        .seat(scheduleSeat.getSeat())
                        .scheduleSeatId(scheduleSeat.getId())
                        .price(scheduleSeat.getGrade().getPrice())
                        .build();
                reservationSeatRepository.save(reservationSeat);
            }

            log.info("V2 예매 완료 - reservationId: {}, reservationNumber: {}",
                    reservation.getId(), reservationNumber);

            return ReservationDtoV2.ReserveResponse.success(
                    reservation.getId(),
                    reservationNumber,
                    reservedSeats.size(),
                    totalAmount,
                    paymentDeadline);

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

    /**
     * 예매 번호 생성
     */
    private String generateReservationNumber() {
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "RES-" + date + "-" + random;
    }
}
