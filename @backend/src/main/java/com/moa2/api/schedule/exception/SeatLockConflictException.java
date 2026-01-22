package com.moa2.api.schedule.exception;

/**
 * 좌석 선점 동시성 충돌(409)용 예외
 * - 하나라도 AVAILABLE이 아니면 선점 실패로 처리
 */
public class SeatLockConflictException extends RuntimeException {
    public SeatLockConflictException(String message) {
        super(message);
    }
}

