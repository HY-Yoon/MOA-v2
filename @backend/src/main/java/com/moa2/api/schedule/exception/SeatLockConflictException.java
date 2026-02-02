package com.moa2.api.schedule.exception;

import java.util.List;

/**
 * 좌석 선점 동시성 충돌(409)용 예외
 * - 하나라도 AVAILABLE이 아니면 선점 실패로 처리
 */
public class SeatLockConflictException extends RuntimeException {
    private final List<Long> conflictSeatIds;

    public SeatLockConflictException(String message, List<Long> conflictSeatIds) {
        super(message);
        this.conflictSeatIds = conflictSeatIds;
    }

    public List<Long> getConflictSeatIds() {
        return conflictSeatIds;
    }
}
