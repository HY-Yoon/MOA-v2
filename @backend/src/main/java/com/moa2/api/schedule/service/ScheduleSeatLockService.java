package com.moa2.api.schedule.service;

import com.moa2.api.schedule.exception.SeatLockConflictException;
import com.moa2.api.schedule.exception.SeatNotFoundException;
import com.moa2.api.show.domain.entity.ScheduleSeat;
import com.moa2.api.show.domain.repository.ScheduleSeatRepository;
import com.moa2.api.show.domain.repository.ShowScheduleRepository;
import com.moa2.global.model.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleSeatLockService {

    private final ShowScheduleRepository showScheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    private static final int LOCK_EXPIRATION_MINUTES = 5;

    @Transactional
    public LocalDateTime lockSeats(Long scheduleId, List<Long> seatIds, Long userId) {
        validateInputs(scheduleId, seatIds, userId);

        // 스케줄 확인
        if (!showScheduleRepository.existsById(scheduleId)) {
            throw new IllegalArgumentException("존재하지 않는 스케줄입니다.");
        }

        // 데드락 방지: ID 정렬
        List<Long> sortedSeatIds = new ArrayList<>(seatIds);
        sortedSeatIds.sort(Long::compareTo);

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0);
        LocalDateTime expiresAt = now.plusMinutes(LOCK_EXPIRATION_MINUTES);

        // 1. PESSIMISTIC LOCK 획득
        List<ScheduleSeat> scheduleSeats = scheduleSeatRepository.findByScheduleIdAndSeatIdInForUpdate(scheduleId, sortedSeatIds);

        // 2. 존재하지 않는 좌석 검증
        validateAllSeatsFound(scheduleSeats, sortedSeatIds);

        // 3. 상태 검증 (모두 AVAILABLE이어야 함)
        validateSeatsAvailable(scheduleSeats);

        // 4. 상태 변경 (LOCK)
        for (ScheduleSeat ss : scheduleSeats) {
            ss.lock(userId, expiresAt);
        }
        scheduleSeatRepository.saveAll(scheduleSeats);

        log.info("좌석 선점 성공: scheduleId={}, userId={}, count={}, expiresAt={}",
                scheduleId, userId, sortedSeatIds.size(), expiresAt);

        return expiresAt;
    }

    // --- Private Validation Methods ---

    private void validateInputs(Long scheduleId, List<Long> seatIds, Long userId) {
        if (scheduleId == null) throw new IllegalArgumentException("scheduleId는 필수입니다.");
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (seatIds == null || seatIds.isEmpty()) throw new IllegalArgumentException("seatIds는 최소 1개 이상 필요합니다.");
        if (new HashSet<>(seatIds).size() != seatIds.size()) throw new IllegalArgumentException("seatIds에 중복 값이 포함되어 있습니다.");
    }

    private void validateAllSeatsFound(List<ScheduleSeat> foundSeats, List<Long> requestedIds) {
        if (foundSeats.size() != requestedIds.size()) {
            List<Long> foundIds = foundSeats.stream().map(ScheduleSeat::getId).toList();
            List<Long> missingIds = requestedIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new SeatNotFoundException("요청한 좌석 중 존재하지 않는 좌석이 포함되어 있습니다.", missingIds);
        }
    }

    private void validateSeatsAvailable(List<ScheduleSeat> seats) {
        List<Long> conflictSeatIds = seats.stream()
                .filter(ss -> ss.getStatus() != SeatStatus.AVAILABLE)
                .map(ScheduleSeat::getId)
                .toList();

        if (!conflictSeatIds.isEmpty()) {
            throw new SeatLockConflictException("선점할 수 없는 좌석이 포함되어 있습니다. (이미 선점/예약/판매됨)", conflictSeatIds);
        }
    }
}