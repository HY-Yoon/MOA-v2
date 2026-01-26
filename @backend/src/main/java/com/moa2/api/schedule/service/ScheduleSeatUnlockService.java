package com.moa2.api.schedule.service;

import com.moa2.api.schedule.exception.SeatLockConflictException;
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

/**
 * 좌석 선점 해제(UNLOCK) 서비스
 * - 사용자가 결제 취소/뒤로가기 등을 했을 때 즉시 선점을 해제하기 위한 용도
 * - 동시성 제어: SELECT ... FOR UPDATE(PESSIMISTIC_WRITE)로 schedule_seats를 잠근 뒤 상태 변경
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleSeatUnlockService {

    private final ShowScheduleRepository showScheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    /**
     * 좌석 선점 해제
     * - 요청 좌석이 모두 "내가 선점한 LOCKED" 이거나 "이미 AVAILABLE(해제된 상태)" 일 때만 성공 처리(멱등)
     *
     * @param scheduleId 스케줄 ID
     * @param seatIds 선점 해제할 좌석 ID 목록 (서비스에서 오름차순 정렬 후 락 획득)
     * @param userId 요청 사용자 ID
     */
    @Transactional
    public void unlockSeats(Long scheduleId, List<Long> seatIds, Long userId) {
        if (scheduleId == null) {
            throw new IllegalArgumentException("scheduleId는 필수입니다.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("seatIds는 최소 1개 이상 필요합니다.");
        }
        // 중복 좌석 요청 방지
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new IllegalArgumentException("seatIds에 중복 값이 포함되어 있습니다.");
        }

        // 스케줄 존재 여부 확인
        showScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

        // 데드락 방지: 좌석 ID 오름차순 정렬 후 락 획득
        List<Long> sortedSeatIds = new ArrayList<>(seatIds);
        sortedSeatIds.sort(Long::compareTo);

        // SELECT ... FOR UPDATE (PESSIMISTIC_WRITE)
        List<ScheduleSeat> scheduleSeats =
                scheduleSeatRepository.findByScheduleIdAndSeatIdInForUpdate(scheduleId, sortedSeatIds);

        if (scheduleSeats.size() != sortedSeatIds.size()) {
            log.warn("좌석 선점 해제 실패(존재하지 않는 좌석 포함): scheduleId={}, userId={}, requestedSeatIds={}, fetchedCount={}",
                    scheduleId, userId, sortedSeatIds, scheduleSeats.size());
            throw new IllegalArgumentException("요청한 좌석 중 존재하지 않는 좌석이 포함되어 있습니다.");
        }

        // 정책: 내 LOCKED 또는 이미 AVAILABLE만 성공 (그 외는 충돌 처리)
        for (ScheduleSeat ss : scheduleSeats) {
            if (ss.getStatus() == SeatStatus.AVAILABLE) {
                continue; // 이미 해제됨 (멱등)
            }

            if (ss.getStatus() == SeatStatus.LOCKED && userId.equals(ss.getLockedByUserId())) {
                ss.releaseLock();
                continue;
            }

            log.info("좌석 선점 해제 충돌(409): scheduleId={}, userId={}, seatId={}, status={}, lockedByUserId={}",
                    scheduleId,
                    userId,
                    ss.getSeat().getId(),
                    ss.getStatus(),
                    ss.getLockedByUserId());
            throw new SeatLockConflictException("선점 해제할 수 없는 좌석이 포함되어 있습니다. (내 선점이 아니거나 이미 예약/판매됨)");
        }

        scheduleSeatRepository.saveAll(scheduleSeats);
        log.info("좌석 선점 해제 성공: scheduleId={}, userId={}, seatIds={}", scheduleId, userId, sortedSeatIds);
    }
}

