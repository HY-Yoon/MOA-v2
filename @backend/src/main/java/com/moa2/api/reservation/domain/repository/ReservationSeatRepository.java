package com.moa2.api.reservation.domain.repository;

import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {
    
    /**
     * 예매에 포함된 좌석 목록 조회
     */
    @Query("SELECT rs FROM ReservationSeat rs " +
           "JOIN FETCH rs.seat s " +
           "JOIN FETCH s.section sec " +
           "WHERE rs.reservation = :reservation")
    List<ReservationSeat> findByReservationWithSeat(@Param("reservation") Reservation reservation);
}
