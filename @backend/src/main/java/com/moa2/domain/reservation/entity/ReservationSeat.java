package com.moa2.domain.reservation.entity;

import com.moa2.domain.show.entity.Seat;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reservation_seats")
public class ReservationSeat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [단방향] 예약 정보를 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat; // 공연 도메인의 물리적 좌석

    private Integer price;

    @Builder
    public ReservationSeat(Reservation reservation, Seat seat, Integer price) {
        this.reservation = reservation;
        this.seat = seat;
        this.price = price;
    }
}







