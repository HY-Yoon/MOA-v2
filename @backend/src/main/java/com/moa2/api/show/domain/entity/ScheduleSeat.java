package com.moa2.api.show.domain.entity;

import com.moa2.api.show.domain.entity.Seat;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.entity.ShowSeatGrade;
import com.moa2.global.entity.BaseTimeEntity;
import com.moa2.global.model.SeatStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회차별 좌석 엔티티
 * 각 공연 회차(Schedule)마다 좌석의 상태를 관리
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "schedule_seats", uniqueConstraints = {
        @UniqueConstraint(name = "uq_schedule_seat", columnNames = { "schedule_id", "seat_id" })
}, indexes = {
        @Index(name = "idx_schedule_status", columnList = "schedule_id, status"),
        @Index(name = "idx_locked_user", columnList = "locked_by_user_id, locked_until")
})
public class ScheduleSeat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ShowSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat; // Venue의 물리적 좌석

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", nullable = false)
    private ShowSeatGrade grade; // 이 좌석의 등급 (VIP, R, S, A 등)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status; // AVAILABLE, LOCKED, RESERVED, SOLD

    @Column(name = "locked_by_user_id")
    private Long lockedByUserId; // 좌석을 선점한 사용자 ID

    private LocalDateTime lockedUntil; // 선점 만료 시간 (5분 후)

    @Builder
    public ScheduleSeat(ShowSchedule schedule, Seat seat, ShowSeatGrade grade) {
        this.schedule = schedule;
        this.seat = seat;
        this.grade = grade;
        this.status = SeatStatus.AVAILABLE; // 초기 상태는 사용 가능
    }

    /**
     * 좌석 선점 (LOCKED)
     * 
     * @param userId      선점한 사용자 ID
     * @param lockedUntil 선점 만료 시간
     */
    public void lock(Long userId, LocalDateTime lockedUntil) {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("이미 선점되었거나 판매된 좌석입니다. 현재 상태: " + this.status);
        }
        this.status = SeatStatus.LOCKED;
        this.lockedByUserId = userId;
        this.lockedUntil = lockedUntil;
    }

    /**
     * 좌석 선점 해제 (만료 시)
     */
    public void releaseLock() {
        if (this.status == SeatStatus.LOCKED) {
            this.status = SeatStatus.AVAILABLE;
            this.lockedByUserId = null;
            this.lockedUntil = null;
        }
    }

    /**
     * 좌석 예약 완료 (결제 진행 중)
     */
    public void reserve() {
        if (this.status != SeatStatus.LOCKED) {
            throw new IllegalStateException("선점되지 않은 좌석은 예약할 수 없습니다. 현재 상태: " + this.status);
        }
        this.status = SeatStatus.RESERVED;
    }

    /**
     * 좌석 판매 완료 (결제 완료)
     */
    public void markAsSold() {
        if (this.status != SeatStatus.RESERVED && this.status != SeatStatus.LOCKED) {
            throw new IllegalStateException("예약되지 않은 좌석은 판매할 수 없습니다. 현재 상태: " + this.status);
        }
        this.status = SeatStatus.SOLD;
        // lockedByUserId와 lockedUntil은 유지 (히스토리 추적용)
    }

    /**
     * 예매 취소(환불) 시 판매 완료 좌석을 다시 판매 가능 상태로 되돌림
     */
    public void releaseAfterSaleCancelled() {
        if (this.status != SeatStatus.SOLD) {
            throw new IllegalStateException("판매 완료(SOLD) 좌석만 취소 반환이 가능합니다. 현재 상태: " + this.status);
        }
        this.status = SeatStatus.AVAILABLE;
        this.lockedByUserId = null;
        this.lockedUntil = null;
    }

    /**
     * 좌석 선점이 만료되었는지 확인
     */
    public boolean isLockExpired() {
        if (this.status != SeatStatus.LOCKED) {
            return false;
        }
        if (this.lockedUntil == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(this.lockedUntil);
    }

    /**
     * 특정 사용자가 선점한 좌석인지 확인
     * - LOCKED: V1 흐름 (선점 후 결제 대기)
     * - RESERVED: V2 흐름 (lock → reserve 순서로 상태 전이됨)
     */
    public boolean isLockedBy(Long userId) {
        return (this.status == SeatStatus.LOCKED || this.status == SeatStatus.RESERVED)
                && this.lockedByUserId != null
                && this.lockedByUserId.equals(userId);
    }
}
