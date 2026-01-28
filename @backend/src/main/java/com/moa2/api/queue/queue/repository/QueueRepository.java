package com.moa2.api.queue.queue.repository;

import com.moa2.api.queue.queue.entity.Queue;
import com.moa2.global.model.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueRepository extends JpaRepository<Queue, Long> {

    /**
     * 특정 사용자와 스케줄에 대한 "활성" 대기열 조회 (최신 1건)
     * - WAITING/READY 중 가장 최근(createdAt desc) 1건을 반환
     */
    Optional<Queue> findTopByUserIdAndScheduleIdAndStatusInOrderByCreatedAtDesc(
            Long userId,
            Long scheduleId,
            Collection<QueueStatus> statuses
    );

    /**
     * 특정 사용자와 스케줄에 대한 대기열 조회 (모든 상태 중 최신 1건)
     * - 데이터가 누적되어도 NonUniqueResultException 방지
     */
    Optional<Queue> findTopByUserIdAndScheduleIdOrderByCreatedAtDesc(Long userId, Long scheduleId);

    /**
     * 내 앞 대기 인원 수 계산
     * (같은 스케줄, WAITING 상태, 나보다 먼저 생성된 Queue)
     */
    @Query("SELECT COUNT(q) FROM Queue q " +
           "WHERE q.schedule.id = :scheduleId " +
           "AND q.status = 'WAITING' " +
           "AND q.id < :queueId")
    Long countWaitingBefore(
        @Param("scheduleId") Long scheduleId,
        @Param("queueId") Long queueId
    );

    /**
     * 특정 스케줄의 전체 WAITING 상태 인원 수 계산
     */
    @Query("SELECT COUNT(q) FROM Queue q " +
           "WHERE q.schedule.id = :scheduleId " +
           "AND q.status = 'WAITING'")
    Long countTotalWaitingByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * 특정 스케줄의 READY 상태 인원 수
     */
    Long countByScheduleIdAndStatus(Long scheduleId, QueueStatus status);

    /**
     * WAITING 상태가 존재하는 스케줄 ID 목록 조회 (중복 제거)
     */
    @Query("SELECT DISTINCT q.schedule.id FROM Queue q WHERE q.status = 'WAITING'")
    List<Long> findScheduleIdsWithWaiting();

    /**
     * 특정 스케줄의 WAITING 사용자 중 가장 오래된 순서대로 조회
     * - Pageable로 가져올 개수(limit) 제어
     */
    @Query("SELECT q FROM Queue q " +
           "WHERE q.schedule.id = :scheduleId " +
           "AND q.status = 'WAITING' " +
           "ORDER BY q.createdAt ASC")
    List<Queue> findOldestWaitingByScheduleId(
        @Param("scheduleId") Long scheduleId,
        Pageable pageable
    );

    /**
     * READY 상태 중 activeUntil이 지난 대기열을 EXPIRED로 만료 처리
     */
    @Modifying
    @Query("UPDATE Queue q " +
           "SET q.status = 'EXPIRED' " +
           "WHERE q.status = 'READY' " +
           "AND q.activeUntil IS NOT NULL " +
           "AND q.activeUntil < :now")
    int expireReadyQueues(@Param("now") LocalDateTime now);
}
