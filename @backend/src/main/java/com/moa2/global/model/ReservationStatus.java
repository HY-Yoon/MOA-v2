package com.moa2.global.model;

/**
 * 예매 상태
 */
public enum ReservationStatus {
    PENDING,   // 결제 대기 (좌석 선점 상태)
    CONFIRMED, // 예매 확정 (결제 완료)
    CANCELLED  // 예매 취소
}

