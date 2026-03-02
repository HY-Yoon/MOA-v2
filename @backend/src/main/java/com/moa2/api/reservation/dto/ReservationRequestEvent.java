package com.moa2.api.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kafka 메시지 페이로드 — 비동기 예매 처리에 필요한 모든 정보를 담는다.
 * eventId를 통해 멱등성(중복 방어)을 보장한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequestEvent {

    private String eventId;          // 멱등성 보장용 UUID
    private Long userId;
    private Long scheduleId;
    private List<Long> scheduleSeatIds;
    private int totalAmount;
    private int lockTtlMinutes;
}
