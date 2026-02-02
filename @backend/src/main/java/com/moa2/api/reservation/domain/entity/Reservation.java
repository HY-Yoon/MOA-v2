package com.moa2.api.reservation.domain.entity;

import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.user.domain.entity.User;
import com.moa2.global.entity.BaseTimeEntity;
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
public class Reservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(length = 20)
    private ReservationStatus status; // PENDING, CONFIRMED, CANCELLED

    @OneToOne(mappedBy = "reservation", fetch = FetchType.LAZY)
    private Payment payment;

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
    }

    // 비즈니스 로직: 예매 확정
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    // 비즈니스 로직: 예매 취소
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * 예매 상태 변경 (Dirty Checking 활용)
     */
    public void updateStatus(ReservationStatus status) {
        this.status = status;
    }

    /**
     * 취소 마감 일시 계산 (공연 시작 48시간 전)
     */
    public LocalDateTime getCancellationDeadline() {
        return this.showSchedule.getShowDate()
                .atTime(this.showSchedule.getShowTime())
                .minusDays(2);
    }

    /**
     * 취소 가능 여부 조회
     * 조건: (CONFIRMED or SOLD) AND (현재 시각 < 공연 2일 전)
     */
    public boolean isCancellable() {
        boolean validStatus = (this.status == ReservationStatus.CONFIRMED || this.status == ReservationStatus.SOLD);
        if (!validStatus) {
            return false;
        }
        return LocalDateTime.now().isBefore(getCancellationDeadline());
    }

    /**
     * 취소 가능 여부 검증 (불가능하면 예외 발생)
     */
    public void validateCancellable() {
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예매입니다.");
        }
        if (this.status != ReservationStatus.CONFIRMED && this.status != ReservationStatus.SOLD) {
            throw new IllegalStateException("확정(CONFIRMED) 또는 결제완료(SOLD) 상태만 취소할 수 있습니다.");
        }
        if (LocalDateTime.now().isAfter(getCancellationDeadline())) {
            throw new IllegalStateException("취소 가능 기한이 지났습니다. (공연 2일 전까지 취소 가능)");
        }
    }
}
