package com.moa2.domain.show.repository;

import com.moa2.domain.show.entity.ShowSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShowScheduleRepository extends JpaRepository<ShowSchedule, Long> {
    List<ShowSchedule> findByShowId(Long showId);
    
    @Query("SELECT ss FROM ShowSchedule ss WHERE ss.show.id = :showId ORDER BY ss.showDate, ss.showTime")
    List<ShowSchedule> findByShowIdOrderByDateAndTime(@Param("showId") Long showId);
}

