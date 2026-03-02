package com.moa2.global.model;

/**
 * 대기열 상태
 */
public enum QueueStatus {
    WAITING, // 대기 중
    READY, // 준비됨 (입장 토큰 발급)
    EXPIRED, // 만료됨 (토큰 5분 초과)
    COMPLETED, // 예매 완료 (V1 DB용)
    NOT_FOUND // 대기열 미진입 (V2 Redis용)
}
