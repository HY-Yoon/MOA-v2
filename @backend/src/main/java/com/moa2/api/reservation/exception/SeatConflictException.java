package com.moa2.api.reservation.exception;

import lombok.Getter;

import java.util.List;

/**
 * 좌석 선점 충돌 예외
 * - conflictSeatIds: 이미 선점된 좌석 번호 목록 (예: "A-4", "B-7")
 */
@Getter
public class SeatConflictException extends RuntimeException {
    private final List<String> conflictSeatIds;

    public SeatConflictException(List<String> conflictSeatIds) {
        super("이미 선점된 좌석이 있습니다: " + conflictSeatIds);
        this.conflictSeatIds = conflictSeatIds;
    }
}
