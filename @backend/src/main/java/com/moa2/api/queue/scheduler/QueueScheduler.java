package com.moa2.api.queue.scheduler;

import com.moa2.domain.queue.entity.Queue;
import com.moa2.domain.queue.repository.QueueRepository;
import com.moa2.global.model.QueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 대기열 처리 스케줄러
 * - READY 인원 제한 내에서 WAITING → READY 승격
 * - READY 세션 만료(activeUntil) 정리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final QueueRepository queueRepository;

    /**
     * 스케줄(회차) 당 동시에 입장 가능한 최대 READY 인원
     */
    @Value("${queue.max-ready-users:3}")
    private int maxReadyUsers;

    /**
     * 1회 스케줄러 실행 시, WAITING → READY로 승격할 최대 인원
     */
    @Value("${queue.activate-batch-size:5}")
    private int activateBatchSize;

    /**
     * READY 상태 유지 시간(분) - 이 시간이 지나면 EXPIRED로 처리
     */
    @Value("${queue.ready-ttl-minutes:5}")
    private int readyTtlMinutes;

    /**
     * 3초마다 대기열 처리
     */
//    @Scheduled(fixedDelayString = "${queue.scheduler.fixed-delay-ms:10000}")
    @Transactional
    public void processQueue() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0);

        // 1) READY 세션 만료 처리
        int expired = queueRepository.expireReadyQueues(now);
        if (expired > 0) {
            log.info("대기열 READY 만료 처리: expiredCount={}", expired);
        }

        // 2) WAITING이 존재하는 스케줄 목록 조회
        List<Long> scheduleIds = queueRepository.findScheduleIdsWithWaiting();
        if (scheduleIds.isEmpty()) {
            return;
        }

        // 3) 스케줄별로 WAITING → READY 승격
        for (Long scheduleId : scheduleIds) {
            long currentReady = queueRepository.countByScheduleIdAndStatus(scheduleId, QueueStatus.READY);
            long availableSlots = (long) maxReadyUsers - currentReady;

            if (availableSlots <= 0) {
                continue;
            }

            int toActivate = (int) Math.min(Math.min(availableSlots, (long) activateBatchSize), (long) Integer.MAX_VALUE);
            if (toActivate <= 0) {
                continue;
            }

            List<Queue> waitingQueues = queueRepository.findOldestWaitingByScheduleId(
                    scheduleId,
                    PageRequest.of(0, toActivate)
            );

            if (waitingQueues.isEmpty()) {
                continue;
            }

            LocalDateTime activeUntil = now.plusMinutes(readyTtlMinutes);
            for (Queue q : waitingQueues) {
                q.activate(activeUntil);
            }
            queueRepository.saveAll(waitingQueues);

            log.info("대기열 승격 처리: scheduleId={}, activatedCount={}, activeUntil={}, currentReady={}, maxReadyUsers={}",
                    scheduleId, waitingQueues.size(), activeUntil, currentReady, maxReadyUsers);
        }
    }
}

