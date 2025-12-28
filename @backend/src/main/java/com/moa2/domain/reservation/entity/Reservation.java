package com.moa2.domain.reservation.entity;

import com.moa2.domain.show.entity.ShowSchedule;
import com.moa2.domain.user.entity.User;
import com.moa2.global.model.ReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reservations")
public class Reservation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ShowSchedule showSchedule;

    @Column(unique = true, nullable = false)
    private String reservationNumber;

    private Integer totalAmount;
    private Integer seatCount;

    private String bookerName;
    private String bookerPhone;
    private String bookerEmail;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status; // PENDING, CONFIRMED, CANCELLED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;

    @Builder
    public Reservation(User user, ShowSchedule showSchedule, String reservationNumber, 
                       Integer totalAmount, Integer seatCount, 
                       String bookerName, String bookerPhone, String bookerEmail) {
        this.user = user;
        this.showSchedule = showSchedule;
        this.reservationNumber = reservationNumber;
        this.totalAmount = totalAmount;
        this.seatCount = seatCount;
        this.bookerName = bookerName;
        this.bookerPhone = bookerPhone;
        this.bookerEmail = bookerEmail;
        this.status = ReservationStatus.PENDING; // 초기 상태
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직: 예매 확정
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 로직: 예매 취소
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}

