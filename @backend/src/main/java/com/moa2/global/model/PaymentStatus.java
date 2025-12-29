package com.moa2.global.model;

/**
 * 결제 상태
 */
public enum PaymentStatus {
    PENDING,   // 결제 요청 중
    COMPLETED, // 결제 승인 완료
    FAILED,    // 결제 실패
    CANCELLED  // 결제 취소 (환불)
}


