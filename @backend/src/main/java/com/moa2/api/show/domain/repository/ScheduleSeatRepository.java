package com.moa2.api.show.domain.repository;

import com.moa2.api.show.domain.entity.ScheduleSeat;
import com.moa2.global.model.SeatStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleSeatRepository extends JpaRepository<ScheduleSeat, Long> {

       /**
        * 회차별 좌석 목록 조회
        */
       List<ScheduleSeat> findByScheduleId(Long scheduleId);

       /**
        * 회차별 좌석 배치도 조회 (좌석/등급/구역까지 fetch join)
        * - 좌석 배치도 API에서 N+1 방지용
        */
       @Query("SELECT ss FROM ScheduleSeat ss " +
                     "JOIN FETCH ss.seat s " +
                     "LEFT JOIN FETCH ss.grade g " +
                     "LEFT JOIN FETCH g.section sec " +
                     "WHERE ss.schedule.id = :scheduleId")
       List<ScheduleSeat> findSeatMapByScheduleId(@Param("scheduleId") Long scheduleId);

       @Modifying
       @Query("DELETE FROM ScheduleSeat ss WHERE ss.schedule.id = :scheduleId")
       int deleteByScheduleId(@Param("scheduleId") Long scheduleId);

       @Modifying
       @Query("DELETE FROM ScheduleSeat ss WHERE ss.schedule.id IN :scheduleIds")
       int deleteByScheduleIdIn(@Param("scheduleIds") List<Long> scheduleIds);



       /**
        * schedule_seat_id로 직접 조회 (비관적 락)
        * 결제 검증 시 사용 - 정확한 schedule_seat_id로 조회
        */
       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @QueryHints({
                     @QueryHint(name = "javax.persistence.lock.timeout", value = "3000")
       })
       @Query("SELECT ss FROM ScheduleSeat ss " +
                     "WHERE ss.schedule.id = :scheduleId " +
                     "AND ss.id IN :scheduleSeatIds " +
                     "ORDER BY ss.id ASC")
       List<ScheduleSeat> findByScheduleIdAndScheduleSeatIdsForUpdate(
                     @Param("scheduleId") Long scheduleId,
                     @Param("scheduleSeatIds") List<Long> scheduleSeatIds);

       /**
        * 만료된 좌석 선점/예약 해제
        * (LOCKED 또는 RESERVED 상태이면서 lockedUntil이 지난 좌석들)
        */
       @Modifying
       @Query("UPDATE ScheduleSeat ss " +
                     "SET ss.status = 'AVAILABLE', " +
                     "ss.lockedByUserId = null, " +
                     "ss.lockedUntil = null " +
                     "WHERE ss.status IN ('LOCKED', 'RESERVED') " +
                     "AND ss.lockedUntil < :now")
       int releaseExpiredLocks(@Param("now") LocalDateTime now);

       /**
        * 특정 회차의 상태별 좌석 수 조회
        */
       Long countByScheduleIdAndStatus(Long scheduleId, SeatStatus status);

       /**
        * 특정 회차의 전체 좌석 수 조회
        */
       Long countByScheduleId(Long scheduleId);

       /**
        * 회차별 좌석 등급 통계 (구역ID, 구역명, 가격, 잔여석, 전체석)
        */
       @Query("SELECT " +
                     "g.section.id, " +
                     "g.section.name, " +
                     "g.price, " +
                     "SUM(CASE WHEN ss.status = 'AVAILABLE' THEN 1 ELSE 0 END), " +
                     "COUNT(ss) " +
                     "FROM ScheduleSeat ss " +
                     "JOIN ss.grade g " +
                     "WHERE ss.schedule.id = :scheduleId " +
                     "GROUP BY g.section.id, g.section.name, g.price")
       List<Object[]> countByGradeForSchedule(@Param("scheduleId") Long scheduleId);

       /**
        * 여러 회차의 좌석 등급별 통계 조회 (N+1 방지)
        * 반환: [scheduleId, sectionId, sectionName, price, remainingSeats, totalSeats]
        */
       @Query("SELECT " +
                     "ss.schedule.id, " +
                     "g.section.id, " +
                     "g.section.name, " +
                     "g.price, " +
                     "SUM(CASE WHEN ss.status = 'AVAILABLE' THEN 1 ELSE 0 END), " +
                     "COUNT(ss) " +
                     "FROM ScheduleSeat ss " +
                     "JOIN ss.grade g " +
                     "WHERE ss.schedule.id IN :scheduleIds " +
                     "GROUP BY ss.schedule.id, g.section.id, g.section.name, g.price")
       List<Object[]> countSeatGradeStatsByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);

       /**
        * 여러 회차의 전체 좌석 수 및 잔여 좌석 수 조회
        * 반환: [scheduleId, totalSeats, remainingSeats]
        */
       @Query("SELECT " +
                     "ss.schedule.id, " +
                     "COUNT(ss), " +
                     "SUM(CASE WHEN ss.status = 'AVAILABLE' THEN 1 ELSE 0 END) " +
                     "FROM ScheduleSeat ss " +
                     "WHERE ss.schedule.id IN :scheduleIds " +
                     "GROUP BY ss.schedule.id")
       List<Object[]> countTotalAndRemainingSeatsByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);
}
