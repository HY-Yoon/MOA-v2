package com.moa2.domain.reservation.entity;

import com.moa2.global.model.PaymentMethod;
import com.moa2.global.model.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payments")
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [단방향] 결제가 예약을 참조 (1:1)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @Column(unique = true, nullable = false)
    private String orderId;

    private String paymentKey;
    private Integer amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // PENDING, COMPLETED, FAILED

    private String failureReason;

    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;

    @Builder
    public Payment(Reservation reservation, String orderId, Integer amount) {
        this.reservation = reservation;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING; // 초기 상태
        this.requestedAt = LocalDateTime.now();
    }

    // 결제 승인 처리
    public void approve(String paymentKey, PaymentMethod method) {
        this.paymentKey = paymentKey;
        this.paymentMethod = method;
        this.status = PaymentStatus.COMPLETED;
        this.approvedAt = LocalDateTime.now();
    }

    // 결제 실패 처리
    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }
    
    // 결제 취소 처리
    public void cancel(String reason) {
        this.status = PaymentStatus.CANCELLED;
        this.failureReason = reason; // 취소 사유 저장 용도
    }
}

