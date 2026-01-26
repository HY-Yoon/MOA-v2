package com.moa2.api.queue.service;

import com.moa2.api.queue.dto.QueueDto;
import com.moa2.api.queue.queue.entity.Queue;
import com.moa2.api.queue.queue.repository.QueueRepository;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.repository.ShowScheduleRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.model.QueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueService {

    private final QueueRepository queueRepository;
    private final ShowScheduleRepository showScheduleRepository;
    private final UserRepository userRepository;

    /**
     * 대기열 진입
     * @param userId 사용자 ID
     * @param request 진입 요청 (scheduleId)
     * @return 대기열 정보 (queueId, position)
     */
    @Transactional
    public QueueDto.EnterResponse enterQueue(Long userId, QueueDto.EnterRequest request) {
        log.info("대기열 진입 요청 - userId: {}, scheduleId: {}", userId, request.scheduleId());

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. 스케줄 조회
        ShowSchedule schedule = showScheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

        // 3. 이미 대기열에 등록되어 있는지 확인 (WAITING 또는 READY 상태)
        Optional<Queue> existingQueue = queueRepository.findTopByUserIdAndScheduleIdAndStatusInOrderByCreatedAtDesc(
                userId,
                request.scheduleId(),
                List.of(QueueStatus.WAITING, QueueStatus.READY)
        );

        Queue queue;
        if (existingQueue.isPresent()) {
            // 이미 대기열에 있음 - 기존 정보 반환
            queue = existingQueue.get();
            log.info("이미 대기열에 등록된 사용자 - queueId: {}, status: {}", queue.getId(), queue.getStatus());
        } else {
            // 새로 대기열 등록
            queue = Queue.builder()
                    .user(user)
                    .schedule(schedule)
                    .build();
            queueRepository.save(queue);
            log.info("대기열 등록 완료 - queueId: {}", queue.getId());
        }

        // 4. 내 앞 대기 인원 수 계산
        Long position = queueRepository.countWaitingBefore(request.scheduleId(), queue.getId());

        log.info("대기 순번 - position: {}", position);

        return QueueDto.EnterResponse.of(queue.getId(), position);
    }

    /**
     * 대기 상태 조회 (scheduleId 포함)
     * @param userId 사용자 ID
     * @param scheduleId 스케줄 ID
     * @return 대기열 상태 정보
     */
    public QueueDto.StatusResponse getQueueStatus(Long userId, Long scheduleId) {
        log.debug("대기 상태 조회 - userId: {}, scheduleId: {}", userId, scheduleId);

        // 1. 대기열 조회 (활성 우선, 없으면 최신 1건)
        Queue queue = queueRepository.findTopByUserIdAndScheduleIdAndStatusInOrderByCreatedAtDesc(
                        userId,
                        scheduleId,
                        List.of(QueueStatus.WAITING, QueueStatus.READY)
                )
                .or(() -> queueRepository.findTopByUserIdAndScheduleIdOrderByCreatedAtDesc(userId, scheduleId))
                .orElseThrow(() -> new IllegalArgumentException("대기열 정보를 찾을 수 없습니다. 먼저 대기열에 진입해주세요."));

        // 2. 상태별 응답 생성
        QueueStatus status = queue.getStatus();
        
        switch (status) {
            case WAITING:
                // 내 앞 대기 인원 수 계산
                Long position = queueRepository.countWaitingBefore(scheduleId, queue.getId());
                log.debug("WAITING 상태 - position: {}", position);
                return QueueDto.StatusResponse.waiting(position);

            case READY:
                // 입장 가능 상태인지 확인
                if (!queue.isActiveSessionValid()) {
                    // 시간 초과로 만료
                    queue.expire();
                    queueRepository.save(queue);
                    log.info("대기열 세션 만료 - queueId: {}", queue.getId());
                    return QueueDto.StatusResponse.expired();
                }
                log.debug("READY 상태 - activeUntil: {}", queue.getActiveUntil());
                return QueueDto.StatusResponse.ready(queue.getActiveUntil());

            case EXPIRED:
                log.debug("EXPIRED 상태");
                return QueueDto.StatusResponse.expired();

            case COMPLETED:
                log.debug("COMPLETED 상태");
                return QueueDto.StatusResponse.completed();

            default:
                throw new IllegalStateException("알 수 없는 대기열 상태입니다: " + status);
        }
    }
}
