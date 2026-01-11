package com.moa2.domain.reservation.repository;

import com.moa2.domain.reservation.entity.Payment;
import com.moa2.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * 예매에 대한 결제 정보 조회
     */
    Optional<Payment> findByReservation(Reservation reservation);
}
