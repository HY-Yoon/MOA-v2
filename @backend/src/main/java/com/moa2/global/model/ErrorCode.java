package com.moa2.global.model;

/**
 * API 에러 코드 표준화 ENUM
 * - 실패 응답의 data.code 필드에 사용
 */
public enum ErrorCode {
    BAD_REQUEST, // 요청 좌석이 존재하지 않을 때
    QUEUE_EXPIRED, // 대기열 세션 만료 (토큰 만료/이미 사용)
    SEAT_CONFLICT // 선점 좌석 충돌
}
