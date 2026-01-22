package com.moa2.api.schedule.service;

import com.moa2.api.schedule.exception.SeatLockConflictException;
import com.moa2.domain.show.entity.ScheduleSeat;
import com.moa2.domain.show.repository.ScheduleSeatRepository;
import com.moa2.domain.show.repository.ShowScheduleRepository;
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

/**
 * 좌석 선점(LOCK) 서비스
 * - 동시성 제어: SELECT ... FOR UPDATE(PESSIMISTIC_WRITE)로 schedule_seats를 잠근 뒤 상태 변경
 * - 규칙: 요청 좌석이 모두 AVAILABLE일 때만 LOCKED로 변경
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleSeatLockService {

    private final ShowScheduleRepository showScheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;

    /**
     * 좌석 선점 (5분)
     *
     * @param scheduleId 스케줄 ID
     * @param seatIds 선점할 좌석 ID 목록 (서비스에서 오름차순 정렬 후 락 획득)
     * @param userId 선점 사용자 ID
     * @return 선점 만료 시각
     */
    @Transactional
    public LocalDateTime lockSeats(Long scheduleId, List<Long> seatIds, Long userId) {
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

        // 데드락 방지: 좌석 ID 오름차순으로 정렬 후 락 획득
        List<Long> sortedSeatIds = new ArrayList<>(seatIds);
        sortedSeatIds.sort(Long::compareTo);

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0);
        LocalDateTime expiresAt = now.plusMinutes(5);

        // SELECT ... FOR UPDATE (PESSIMISTIC_WRITE)
        List<ScheduleSeat> scheduleSeats =
                scheduleSeatRepository.findByScheduleIdAndSeatIdInForUpdate(scheduleId, sortedSeatIds);

        if (scheduleSeats.size() != sortedSeatIds.size()) {
            log.warn("좌석 선점 실패(존재하지 않는 좌석 포함): scheduleId={}, userId={}, requestedSeatIds={}, fetchedCount={}",
                    scheduleId, userId, sortedSeatIds, scheduleSeats.size());
            throw new IllegalArgumentException("요청한 좌석 중 존재하지 않는 좌석이 포함되어 있습니다.");
        }

        // 모두 AVAILABLE일 때만 LOCKED로 변경
        boolean allAvailable = scheduleSeats.stream().allMatch(ss -> ss.getStatus() == SeatStatus.AVAILABLE);
        if (!allAvailable) {
            log.info("좌석 선점 충돌(409): scheduleId={}, userId={}, seatIds={}, statuses={}",
                    scheduleId,
                    userId,
                    sortedSeatIds,
                    scheduleSeats.stream().map(ScheduleSeat::getStatus).toList());
            throw new SeatLockConflictException("선점할 수 없는 좌석이 포함되어 있습니다. (이미 선점/예약/판매됨)");
        }

        for (ScheduleSeat ss : scheduleSeats) {
            ss.lock(userId, expiresAt);
        }
        scheduleSeatRepository.saveAll(scheduleSeats);

        log.info("좌석 선점 성공: scheduleId={}, userId={}, seatIds={}, expiresAt={}",
                scheduleId, userId, sortedSeatIds, expiresAt);

        return expiresAt;
    }
}

