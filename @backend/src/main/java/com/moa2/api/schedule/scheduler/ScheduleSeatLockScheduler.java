package com.moa2.api.schedule.scheduler;

import com.moa2.api.show.domain.repository.ScheduleSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 좌석 선점 만료 스케줄러
 * - 10초마다 LOCKED && lockedUntil < now 인 좌석을 AVAILABLE로 되돌림
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleSeatLockScheduler {

    private final ScheduleSeatRepository scheduleSeatRepository;

    /**
     * 10초마다 만료된 선점을 자동 해제
     */
    @Scheduled(fixedDelayString = "${schedule-seat.lock.release.fixed-delay-ms:10000}")
    @Transactional
    public void releaseExpiredSeatLocks() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0);
        int released = scheduleSeatRepository.releaseExpiredLocks(now);
        if (released > 0) {
            log.info("만료 좌석 선점 해제: releasedCount={}, now={}", released, now);
        }
    }
}

