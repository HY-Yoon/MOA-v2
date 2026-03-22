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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleSeatUnlockService {

    private final ShowScheduleRepository showScheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    @Transactional
    public void unlockSeats(Long scheduleId, List<Long> scheduleSeatIds, Long userId) {
        validateInputs(scheduleId, scheduleSeatIds, userId);

        if (!showScheduleRepository.existsById(scheduleId)) {
            throw new IllegalArgumentException("존재하지 않는 스케줄입니다.");
        }

        // 데드락 방지: ID 정렬
        List<Long> sortedSeatIds = new ArrayList<>(scheduleSeatIds);
        sortedSeatIds.sort(Long::compareTo);

        // 1. PESSIMISTIC LOCK 획득
        List<ScheduleSeat> scheduleSeats = scheduleSeatRepository.findByScheduleIdAndScheduleSeatIdsForUpdate(scheduleId, sortedSeatIds);

        // 2. 존재하지 않는 좌석 검증
        validateAllSeatsFound(scheduleSeats, sortedSeatIds);

        // 3. 해제 가능 여부 검증 (멱등성 + 권한 체크)
        validateUnlockable(scheduleSeats, userId);

        // 4. 상태 변경 (UNLOCK)
        for (ScheduleSeat ss : scheduleSeats) {
            // 이미 AVAILABLE이면 pass, 내 선점이면 release
            if (ss.getStatus() == SeatStatus.LOCKED && userId.equals(ss.getLockedByUserId())) {
                ss.releaseLock();
            }
        }
        scheduleSeatRepository.saveAll(scheduleSeats);

        log.info("좌석 선점 해제 성공: scheduleId={}, userId={}, count={}", scheduleId, userId, sortedSeatIds.size());
    }


    private void validateInputs(Long scheduleId, List<Long> scheduleSeatIds, Long userId) {
        if (scheduleId == null) throw new IllegalArgumentException("scheduleId는 필수입니다.");
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (scheduleSeatIds == null || scheduleSeatIds.isEmpty()) throw new IllegalArgumentException("scheduleSeatIds는 최소 1개 이상 필요합니다.");
        if (new HashSet<>(scheduleSeatIds).size() != scheduleSeatIds.size()) throw new IllegalArgumentException("scheduleSeatIds에 중복 값이 포함되어 있습니다.");
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

    private void validateUnlockable(List<ScheduleSeat> seats, Long userId) {
        List<Long> conflictSeatIds = new ArrayList<>();
        for (ScheduleSeat ss : seats) {
            if (ss.getStatus() == SeatStatus.AVAILABLE) {
                continue; // 이미 해제됨 (성공으로 간주)
            }
            if (ss.getStatus() == SeatStatus.LOCKED && userId.equals(ss.getLockedByUserId())) {
                continue; // 내 선점 (해제 가능)
            }
            // 그 외: 다른 사람의 LOCKED, RESERVED, SOLD
            conflictSeatIds.add(ss.getId());
        }

        if (!conflictSeatIds.isEmpty()) {
            throw new SeatLockConflictException("선점 해제할 수 없는 좌석이 포함되어 있습니다. (내 선점이 아니거나 이미 예약/판매됨)", conflictSeatIds);
        }
    }
}