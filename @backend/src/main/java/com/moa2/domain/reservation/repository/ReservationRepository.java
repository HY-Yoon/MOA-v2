package com.moa2.domain.reservation.repository;

import com.moa2.domain.reservation.entity.Reservation;
import com.moa2.domain.show.entity.ShowSchedule;
import com.moa2.global.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.showSchedule.id = :scheduleId AND r.status != 'CANCELLED'")
    Long countByScheduleId(@Param("scheduleId") Long scheduleId);
}

