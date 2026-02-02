package com.moa2.api.show.domain.repository;

import com.moa2.api.show.domain.entity.ShowSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShowScheduleRepository extends JpaRepository<ShowSchedule, Long> {
        List<ShowSchedule> findByShowId(Long showId);

        @Query("SELECT ss FROM ShowSchedule ss WHERE ss.show.id = :showId ORDER BY ss.showDate, ss.showTime")
        List<ShowSchedule> findByShowIdOrderByDateAndTime(@Param("showId") Long showId);

        /**
         * 특정 공연의 회차 목록 조회 (date가 있으면 해당 날짜만 필터링)
         */
        @Query("SELECT ss FROM ShowSchedule ss " +
                        "WHERE ss.show.id = :showId " +
                        "AND (CAST(:date AS date) IS NULL OR ss.showDate = :date) " +
                        "ORDER BY ss.showDate, ss.showTime")
        List<ShowSchedule> findByShowIdAndOptionalDate(
                        @Param("showId") Long showId,
                        @Param("date") LocalDate date);

        /**
         * 여러 공연의 모든 일정 조회 (N+1 방지용)
         */
        @Query("SELECT ss FROM ShowSchedule ss WHERE ss.show.id IN :showIds ORDER BY ss.showDate, ss.showTime")
        List<ShowSchedule> findAllByShowIdInOrderByDateAndTime(@Param("showIds") List<Long> showIds);
}
