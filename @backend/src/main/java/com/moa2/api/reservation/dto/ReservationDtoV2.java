package com.moa2.api.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * V2: Redis 기반 예매 DTO
 * - 토큰 검증 + Redisson 분산 락 적용
 */
public class ReservationDtoV2 {

        /**
         * 예매 요청 DTO
         */
        @Builder
        @Schema(description = "V2 예매 요청 (토큰 필수)")
        public record ReserveRequest(
                        @NotNull(message = "스케줄 ID는 필수입니다") @Schema(description = "공연 회차 ID", example = "7", requiredMode = Schema.RequiredMode.REQUIRED) Long scheduleId,

                        @NotEmpty(message = "좌석을 선택해주세요") @Schema(description = "선택한 회차별 좌석 ID 목록 (scheduleSeatId). 좌석 조회 API 응답의 scheduleSeatId 값을 사용하세요.", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED) List<Long> scheduleSeatIds) {
        }

        /**
         * 예매 응답 DTO
         * - success 필드 제거 (ApiResponse.success 와 중복)
         * - orderId 추가 (Mock 결제 및 실제 결제 연동용)
         */
        @Builder
        @Schema(description = "V2 예매 응답")
        public record ReserveResponse(
                        @Schema(description = "예매 ID", example = "1234") Long reservationId,

                        @Schema(description = "예매 번호", example = "RES-20260203-A1B2C3") String reservationNumber,

                        @Schema(description = "결제 주문 ID (Mock/실제 결제 시 사용)", example = "MOA-abc123def456ghi789jk") String orderId,

                        @Schema(description = "선점된 좌석 수", example = "2") int seatCount,

                        @Schema(description = "총 금액", example = "100000") int totalAmount,

                        @Schema(description = "결제 기한 (5분 내)", example = "2026-02-03T11:00:00") LocalDateTime paymentDeadline,

                        @Schema(description = "메시지", example = "좌석 선점 완료! 5분 내에 결제해주세요.") String message) {

                /**
                 * 예매 성공 응답 팩토리
                 */
                public static ReserveResponse success(
                                Long reservationId,
                                String reservationNumber,
                                String orderId,
                                int seatCount,
                                int totalAmount,
                                LocalDateTime paymentDeadline) {
                        return ReserveResponse.builder()
                                        .reservationId(reservationId)
                                        .reservationNumber(reservationNumber)
                                        .orderId(orderId)
                                        .seatCount(seatCount)
                                        .totalAmount(totalAmount)
                                        .paymentDeadline(paymentDeadline)
                                        .message("좌석 선점 완료! "
                                                        + paymentDeadline
                                                                        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                                                        + "까지 결제해주세요.")
                                        .build();
                }
        }
}
