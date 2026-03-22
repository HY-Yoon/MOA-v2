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

import org.springframework.data.redis.core.RedisCallback;

/**
 * V2: Redisson 분산 락 기반 예매 서비스
 *
 * [변경 후 흐름]
 * 1. Redis 분산 락 획득 + 좌석 유효성 검증
 * 2. Redis에 좌석 선점 상태 저장 (SETNX + TTL)
 * 3. 즉시 200 OK 응답 (DB Write 없음!)
 * 4. /order API에서 예약자 정보와 함께 DB 저장
 */
@Slf4j
@Profile("v2")
@Service
@RequiredArgsConstructor
public class ReservationServiceV2 {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ScheduleSeatRepository scheduleSeatRepository;

    @Value("${queue.token.ttl-minutes:5}")
    private int lockTtlMinutes;

    private static final String SEAT_LOCK_PREFIX = "seat:lock:";
    private static final String SEAT_STATUS_PREFIX = "seat:status:";

    /**
     * [1단계] 좌석 선점 (Redis Only — DB Write 없음)
     * - Redis 분산 락으로 좌석 동시성 제어
     * - Redis에 선점 상태만 저장 (TTL 5분)
     * - 즉시 200 응답
     */
    public ReservationDtoV2.ReserveSeatResponse reserveSeats(
            Long userId,
            Long scheduleId,
            List<Long> scheduleSeatIds) {
        log.info("V2 좌석 선 점 시작 - userId: {}, scheduleId: {}, scheduleSeatIds: {}", userId, scheduleId, scheduleSeatIds);

        // 1. 좌석 ID 정렬 (데드락 방지)
        List<Long> sortedSeatIds = new ArrayList<>(scheduleSeatIds);
        sortedSeatIds.sort(Long::compareTo);

        List<Long> validatedSeatIds = new ArrayList<>();
        List<RLock> acquiredLocks = new ArrayList<>();
        List<String> conflictSeatNumbers = new ArrayList<>();
        int totalAmount = 0;

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

                    if (cachedStatus != null) {
                        // 이미 누군가 선점했거나 판매된 좌석
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

                    // Redis에 선점 저장: seat:status:{seatId} = userId (TTL)
                    redisTemplate.opsForValue().set(
                            statusKey,
                            String.valueOf(userId),
                            lockTtlMinutes, TimeUnit.MINUTES);

                    validatedSeatIds.add(seatId);
                    totalAmount += scheduleSeat.getGrade().getPrice();
                    log.debug("좌석 선점 성공: seatId={}", seatId);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("좌석 선점 중 인터럽트 발생", e);
                }
            }

            // 충돌된 좌석이 있으면 SeatConflictException 발생
            if (!conflictSeatNumbers.isEmpty()) {
                // 이미 선점한 좌석들의 Redis 키 롤백
                for (Long seatId : validatedSeatIds) {
                    redisTemplate.delete(SEAT_STATUS_PREFIX + seatId);
                }
                throw new SeatConflictException(conflictSeatNumbers);
            }

            // 선점 결과 반환 (DB Write 없음!)
            long remainingSeconds = (long) lockTtlMinutes * 60;

            log.info("V2 좌석 선점 완료 - userId: {}, seats: {}, totalAmount: {}", userId, validatedSeatIds, totalAmount);

            return ReservationDtoV2.ReserveSeatResponse.of(
                    validatedSeatIds.size(), remainingSeconds, totalAmount);

        } finally {
            // 락 해제 (항상 실행)
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
     * [2단계] 주문 생성 전 Redis 선점 유효성 확인 (Pipeline)
     * - N개 좌석을 Redis 왕복 1회로 확인
     */
    public void validateSeatHold(Long userId, List<Long> scheduleSeatIds) {
        // Pipeline MGET - 왕복 1회
        List<String> keys = scheduleSeatIds.stream()
                .map(seatId -> SEAT_STATUS_PREFIX + seatId)
                .toList();

        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                connection.stringCommands().get(key.getBytes());
            }
            return null;
        });

        for (int i = 0; i < scheduleSeatIds.size(); i++) {
            Object result = results.get(i);
            Long seatId = scheduleSeatIds.get(i);

            if (result == null) {
                throw new IllegalStateException("좌석 선점 시간이 만료되었습니다. 좌석 ID: " + seatId);
            }
            if (!result.toString().equals(String.valueOf(userId))) {
                throw new IllegalStateException("다른 사용자가 선점한 좌석입니다. 좌석 ID: " + seatId);
            }
        }
    }

    /**
     * 좌석 선점 해제 (결제 실패/취소 시 호출, Pipeline)
     */
    public void releaseSeatHold(List<Long> scheduleSeatIds) {
        if (scheduleSeatIds.isEmpty()) return;

        // Pipeline DEL - 왕복 1회
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long seatId : scheduleSeatIds) {
                connection.keyCommands().del((SEAT_STATUS_PREFIX + seatId).getBytes());
            }
            return null;
        });
        log.info("좌석 선점 해제 완료: scheduleSeatIds={}", scheduleSeatIds);
    }

    /**
     * [미리보기] 결제 페이지 진입 시 주문 상세 조회
     */
    public ReservationDtoV2.PreviewResponse getPreviewInfo(
            Long userId,
            Long scheduleId,
            List<Long> scheduleSeatIds,
            com.moa2.api.user.domain.entity.User user) {
        
        // 1. 좌석 선점 유효성 확인 및 잔여 시간 조회 (Pipeline - 왕복 2회)
        List<String> statusKeys = scheduleSeatIds.stream()
                .map(seatId -> SEAT_STATUS_PREFIX + seatId)
                .toList();

        // Pipeline GET - 왕복 1회
        List<Object> holdResults = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : statusKeys) {
                connection.stringCommands().get(key.getBytes());
            }
            return null;
        });

        for (int i = 0; i < scheduleSeatIds.size(); i++) {
            Object result = holdResults.get(i);
            Long seatId = scheduleSeatIds.get(i);

            if (result == null) {
                throw new IllegalStateException("좌석 선점 시간이 만료되었습니다. 좌석 ID: " + seatId);
            }
            if (!result.toString().equals(String.valueOf(userId))) {
                throw new IllegalStateException("다른 사용자가 선점한 좌석입니다. 좌석 ID: " + seatId);
            }
        }

        // Pipeline TTL - 왕복 1회
        List<Object> ttlResults = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : statusKeys) {
                connection.keyCommands().ttl(key.getBytes());
            }
            return null;
        });

        long minTtlSeconds = Long.MAX_VALUE;
        for (Object ttlResult : ttlResults) {
            if (ttlResult != null) {
                long ttl = ((Number) ttlResult).longValue();
                if (ttl > 0) {
                    minTtlSeconds = Math.min(minTtlSeconds, ttl);
                }
            }
        }

        if (minTtlSeconds == Long.MAX_VALUE) {
            minTtlSeconds = 0;
        }

        // 2. DB에서 좌석 및 스케줄, 공연 정보 조회
        List<ScheduleSeat> seats = scheduleSeatIds.stream()
                .map(id -> scheduleSeatRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다: " + id)))
                .toList();

        if (seats.isEmpty()) {
            throw new IllegalArgumentException("선택된 좌석이 없습니다.");
        }

        com.moa2.api.show.domain.entity.ShowSchedule schedule = seats.get(0).getSchedule();
        if (!schedule.getId().equals(scheduleId)) {
            throw new IllegalArgumentException("해당 스케줄의 좌석이 아닙니다.");
        }

        com.moa2.api.show.domain.entity.Show show = schedule.getShow();

        // 3. 금액 계산 (수수료는 기본 4000원으로 가정, 기획에 맞게 수정 가능)
        int ticketAmount = seats.stream().mapToInt(seat -> seat.getGrade().getPrice()).sum();
        int bookingFee = 4000; 
        int totalAmount = ticketAmount + bookingFee;

        // 4. 좌석 상세 정보 맵핑
        List<ReservationDtoV2.PreviewResponse.SeatPreviewInfo> seatPreviewInfos = seats.stream()
                .map(scheduleSeat -> ReservationDtoV2.PreviewResponse.SeatPreviewInfo.builder()
                        .scheduleSeatId(scheduleSeat.getId())
                        .gradeName(scheduleSeat.getGrade().getSection().getName()) // 구역명 (R석, VIP 등)
                        .seatNumber(scheduleSeat.getSeat().getSeatRow() + "열 " + scheduleSeat.getSeat().getSeatNumber() + "번")
                        .price(scheduleSeat.getGrade().getPrice())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        // 5. 기본 예약자 정보
        ReservationDtoV2.PreviewResponse.BookerInfo defaultBooker = 
                ReservationDtoV2.PreviewResponse.BookerInfo.builder()
                        .name(user.getName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .build();

        // 6. 응답 DTO 생성
        return ReservationDtoV2.PreviewResponse.builder()
                .title(show.getTitle())
                .venueName(show.getVenue() != null ? show.getVenue().getName() : "미정")
                .showDate(schedule.getShowDate() != null ? schedule.getShowDate().toString() : "")
                .showTime(schedule.getShowTime() != null ? schedule.getShowTime().toString() : "")
                .bookingFee(bookingFee)
                .ticketAmount(ticketAmount)
                .totalAmount(totalAmount)
                .paymentDeadline(java.time.LocalDateTime.now().plusSeconds(minTtlSeconds))
                .seats(seatPreviewInfos)
                .defaultBooker(defaultBooker)
                .build();
    }
}
