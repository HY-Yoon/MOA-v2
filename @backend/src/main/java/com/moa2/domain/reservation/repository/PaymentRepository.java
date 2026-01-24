package com.moa2.domain.reservation.repository;

import com.moa2.domain.reservation.entity.Payment;
import com.moa2.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * 예매에 대한 결제 정보 조회
     */
    Optional<Payment> findByReservation(Reservation reservation);

    /**
     * 주문번호(orderId)로 결제 정보 조회
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * 주문번호로 결제 정보 조회 (예약 정보 포함)
     */
    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.reservation r " +
           "JOIN FETCH r.user u " +
           "JOIN FETCH r.showSchedule sch " +
           "JOIN FETCH sch.show s " +
           "WHERE p.orderId = :orderId")
    Optional<Payment> findByOrderIdWithReservation(@Param("orderId") String orderId);
}
