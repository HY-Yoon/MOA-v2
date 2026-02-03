package com.moa2.api.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
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

            @NotEmpty(message = "좌석을 선택해주세요") @Schema(description = "선택한 좌석 ID 목록", example = "[101, 102]", requiredMode = Schema.RequiredMode.REQUIRED) List<Long> seatIds,

            @NotBlank(message = "예매자 이름은 필수입니다") @Schema(description = "예매자 이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED) String bookerName,

            @NotBlank(message = "연락처는 필수입니다") @Schema(description = "예매자 연락처", example = "010-1234-5678", requiredMode = Schema.RequiredMode.REQUIRED) String bookerPhone,

            @Schema(description = "예매자 이메일", example = "hong@example.com") String bookerEmail) {
    }

    /**
     * 예매 응답 DTO
     */
    @Builder
    @Schema(description = "V2 예매 응답")
    public record ReserveResponse(
            @Schema(description = "예매 성공 여부", example = "true") boolean success,

            @Schema(description = "예매 ID", example = "1234") Long reservationId,

            @Schema(description = "예매 번호", example = "RES-20260203-A1B2C3") String reservationNumber,

            @Schema(description = "선점된 좌석 수", example = "2") int seatCount,

            @Schema(description = "총 금액", example = "100000") int totalAmount,

            @Schema(description = "결제 기한 (5분 내)", example = "2026-02-03T11:00:00") LocalDateTime paymentDeadline,

            @Schema(description = "메시지", example = "좌석 선점 완료! 5분 내에 결제해주세요.") String message) {
        /**
         * 예매 성공 응답
         */
        public static ReserveResponse success(
                Long reservationId,
                String reservationNumber,
                int seatCount,
                int totalAmount,
                LocalDateTime paymentDeadline) {
            return ReserveResponse.builder()
                    .success(true)
                    .reservationId(reservationId)
                    .reservationNumber(reservationNumber)
                    .seatCount(seatCount)
                    .totalAmount(totalAmount)
                    .paymentDeadline(paymentDeadline)
                    .message("좌석 선점 완료! " + paymentDeadline.toLocalTime() + "까지 결제해주세요.")
                    .build();
        }
    }
}
