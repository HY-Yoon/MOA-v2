package com.moa2.domain.reservation.repository;

import com.moa2.domain.reservation.entity.Reservation;
import com.moa2.domain.show.entity.ShowSchedule;
import com.moa2.domain.user.entity.User;
import com.moa2.global.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.showSchedule.id = :scheduleId AND r.status != 'CANCELLED'")
    Long countByScheduleId(@Param("scheduleId") Long scheduleId);
    
    /**
     * 사용자의 특정 상태 예매 목록 조회 (진행중인 예매 확인용)
     */
    List<Reservation> findByUserAndStatusIn(User user, List<ReservationStatus> statuses);
    
    /**
     * 사용자의 모든 예매 목록 조회 (페이지네이션)
     */
    @Query("SELECT r FROM Reservation r " +
           "JOIN FETCH r.showSchedule sch " +
           "JOIN FETCH sch.show s " +
           "WHERE r.user = :user " +
           "ORDER BY r.createdAt DESC")
    Page<Reservation> findByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * 사용자의 특정 상태 예매 목록 조회 (페이지네이션)
     */
    @Query("SELECT r FROM Reservation r " +
           "JOIN FETCH r.showSchedule sch " +
           "JOIN FETCH sch.show s " +
           "WHERE r.user = :user AND r.status = :status " +
           "ORDER BY r.createdAt DESC")
    Page<Reservation> findByUserAndStatus(@Param("user") User user, @Param("status") ReservationStatus status, Pageable pageable);
    
    /**
     * 예매 상세 조회 (사용자 검증용)
     */
    @Query("SELECT r FROM Reservation r " +
           "JOIN FETCH r.showSchedule sch " +
           "JOIN FETCH sch.show s " +
           "JOIN FETCH s.venue v " +
           "WHERE r.id = :reservationId AND r.user = :user")
    Optional<Reservation> findByIdAndUser(@Param("reservationId") Long reservationId, @Param("user") User user);
}

