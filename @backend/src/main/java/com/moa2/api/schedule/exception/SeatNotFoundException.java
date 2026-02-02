package com.moa2.api.schedule.exception;

import java.util.List;

/**
 * 요청한 좌석이 존재하지 않을 때 발생하는 예외 (400 Bad Request)
 * - data 필드에 존재하지 않는 좌석 ID 목록을 담아 전달
 */
public class SeatNotFoundException extends RuntimeException {
    private final List<Long> notFoundSeatIds;

    public SeatNotFoundException(String message, List<Long> notFoundSeatIds) {
        super(message);
        this.notFoundSeatIds = notFoundSeatIds;
    }

    public List<Long> getNotFoundSeatIds() {
        return notFoundSeatIds;
    }
}
