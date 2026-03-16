package com.moa2.api.reservation.dto;

import com.moa2.global.model.PaymentStatus;
import com.moa2.global.model.ReservationStatus;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Builder
@Schema(description = "예매 내역 검색 조건")
public record ReservationSearchCondition(
        @Parameter(description = "예매 상태 필터") ReservationStatus status,

        @Parameter(description = "결제 상태 필터") PaymentStatus paymentStatus,

        @Parameter(description = "날짜 기준 (RESERVATION: 예매일, SHOW: 공연일)") String dateType,

        @Parameter(description = "조회 시작일") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

        @Parameter(description = "조회 종료일") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

        @Parameter(description = "페이지 번호 (0부터 시작, 기본값: 0)", required = false, example = "0")
        @Schema(defaultValue = "0")
        int page,

        @Parameter(description = "페이지 크기 (기본값: 20)", required = false, example = "20")
        @Schema(defaultValue = "20")
        int size) {
    public ReservationSearchCondition {
        if (size == 0)
            size = 20;
    }
}
