package com.moa2.domain.queue.entity;

import com.moa2.domain.show.entity.ShowSchedule;
import com.moa2.domain.user.entity.User;
import com.moa2.global.entity.BaseTimeEntity;
import com.moa2.global.model.QueueStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대기열 엔티티
 * 공연 예매 시 대기열 관리를 위한 테이블
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "queues",
    indexes = {
        @Index(name = "idx_schedule_status_created", columnList = "schedule_id, status, created_at"),
        @Index(name = "idx_user_schedule", columnList = "user_id, schedule_id")
    }
)
public class Queue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ShowSchedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status; // WAITING, READY, EXPIRED, COMPLETED

    private LocalDateTime activeUntil; // READY 상태에서 언제까지 유효한지 (세션 만료 시간)

    @Builder
    public Queue(User user, ShowSchedule schedule) {
        this.user = user;
        this.schedule = schedule;
        this.status = QueueStatus.WAITING; // 초기 상태는 대기
    }

    /**
     * 대기열 상태를 READY(입장 가능)로 변경
     * @param activeUntil 입장 가능 시간 (예: 현재 시간 + 5분)
     */
    public void activate(LocalDateTime activeUntil) {
        this.status = QueueStatus.READY;
        this.activeUntil = activeUntil;
    }

    /**
     * 대기열 만료 처리
     */
    public void expire() {
        this.status = QueueStatus.EXPIRED;
    }

    /**
     * 대기열 완료 처리 (결제 완료 시)
     */
    public void complete() {
        this.status = QueueStatus.COMPLETED;
    }

    /**
     * READY 상태가 유효한지 확인
     */
    public boolean isActiveSessionValid() {
        if (this.status != QueueStatus.READY) {
            return false;
        }
        if (this.activeUntil == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(this.activeUntil);
    }
}
