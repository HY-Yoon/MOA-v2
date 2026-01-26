package com.moa2.api.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa2.global.model.SeatStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 좌석 스케줄 관련 DTO 모음
 */
public class ScheduleDto {

    /**
     * 좌석 선점 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "좌석 선점 요청")
    public static class SeatLockRequest {

        @NotEmpty(message = "seatIds는 최소 1개 이상 필요합니다")
        @Schema(description = "선점할 좌석 ID 목록", example = "[10, 11]")
        private List<Long> seatIds;
    }

    /**
     * 좌석 선점 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "좌석 선점 응답")
    public static class SeatLockResponse {

        @Schema(description = "선점 성공 여부", example = "true")
        @JsonProperty("isSuccess")
        private boolean isSuccess;

        @Schema(description = "선점 만료 시각 (ISO_LOCAL_DATE_TIME)", example = "2026-01-21T10:10:00")
        private LocalDateTime expiresAt;
    }

    /**
     * 좌석 선점 해제 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "좌석 선점 해제 요청")
    public static class SeatUnlockRequest {

        @NotEmpty(message = "seatIds는 최소 1개 이상 필요합니다")
        @Schema(description = "선점 해제할 좌석 ID 목록", example = "[10, 11]")
        private List<Long> seatIds;
    }

    /**
     * 좌석 선점 해제 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "좌석 선점 해제 응답")
    public static class SeatUnlockResponse {

        @Schema(description = "선점 해제 성공 여부", example = "true")
        @JsonProperty("isSuccess")
        private boolean isSuccess;
    }

    /**
     * 회차 좌석 배치도 조회 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "회차 좌석 배치도 응답")
    public static class SeatsResponse {

        @Schema(description = "스케줄 ID", example = "100")
        private Long scheduleId;

        @Schema(description = "좌석 목록")
        private List<SeatInfo> seats;
    }

    /**
     * 좌석 정보
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "좌석 정보")
    public static class SeatInfo {
        @Schema(description = "좌석 ID", example = "1")
        private Long seatId;

        @Schema(description = "행", example = "A")
        private String row;

        @Schema(description = "열(번호)", example = "1")
        private Integer col;

        @Schema(description = "등급/구역명", example = "VIP")
        private String grade;

        @Schema(description = "가격", example = "150000")
        private Integer price;

        @Schema(description = "좌석 상태", example = "AVAILABLE")
        private SeatStatus status;
    }
}
