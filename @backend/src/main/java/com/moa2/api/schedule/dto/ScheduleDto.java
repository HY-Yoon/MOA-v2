package com.moa2.api.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa2.global.model.SeatStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 좌석 스케줄 관련 DTO 모음 (Record 변환 완료)
 */
public class ScheduleDto {

        /**
         * 좌석 선점 요청 DTO
         */
        @Schema(description = "좌석 선점 요청")
        public record SeatLockRequest(
                        @NotEmpty(message = "seatIds는 최소 1개 이상 필요합니다") @Schema(description = "선점할 좌석 ID 목록", example = "[10, 11]") List<Long> seatIds) {
        }

        /**
         * 좌석 선점 응답 DTO
         */
        @Builder
        @Schema(description = "좌석 선점 응답")
        public record SeatLockResponse(
                        @Schema(description = "선점 성공 여부", example = "true") @JsonProperty("isSuccess") boolean isSuccess,

                        @Schema(description = "선점 만료 시각 (ISO_LOCAL_DATE_TIME)", example = "2026-01-21T10:10:00") LocalDateTime expiresAt) {
        }

        /**
         * 좌석 선점 해제 요청 DTO
         */
        @Schema(description = "좌석 선점 해제 요청")
        public record SeatUnlockRequest(
                        @NotEmpty(message = "seatIds는 최소 1개 이상 필요합니다") @Schema(description = "선점 해제할 좌석 ID 목록", example = "[10, 11]") List<Long> seatIds) {
        }

        /**
         * 좌석 선점 해제 응답 DTO
         */
        @Builder
        @Schema(description = "좌석 선점 해제 응답")
        public record SeatUnlockResponse(
                        @Schema(description = "선점 해제 성공 여부", example = "true") @JsonProperty("isSuccess") boolean isSuccess) {
        }

        /**
         * 회차 좌석 배치도 조회 응답 DTO
         */
        @Builder
        @Schema(description = "회차 좌석 배치도 응답")
        public record SeatsResponse(
                        @Schema(description = "최대 선택 가능 좌석 수", example = "6") Integer maxSelectable,

                        @Schema(description = "좌석 목록") List<SeatInfo> seats) {
        }

        /**
         * 좌석 정보
         */
        @Builder
        @Schema(description = "좌석 정보")
        public record SeatInfo(
                        @Schema(description = "스케줄 좌석 ID (PK)", example = "150") Long scheduleSeatId,

                        @Schema(description = "좌석 ID (구역-번호 형식)", example = "A-1") String seatId,

                        @Schema(description = "구역 ID", example = "A") String sectionId,

                        @Schema(description = "행", example = "A") String row,

                        @Schema(description = "번호", example = "1") Integer number,

                        @Schema(description = "X 좌표", example = "120") Integer x,

                        @Schema(description = "Y 좌표", example = "200") Integer y,

                        @Schema(description = "좌석 상태", example = "AVAILABLE") SeatStatus status) {
        }

        /**
         * 좌석 배치도 조회 응답 DTO (canvas + sections)
         */
        @Builder
        @Schema(description = "좌석 배치도 응답")
        public record SeatMapResponse(
                        @Schema(description = "캔버스 정보") CanvasInfo canvas,

                        @Schema(description = "구역 목록") List<SectionInfo> sections) {
        }

        /**
         * 캔버스 정보
         */
        @Builder
        @Schema(description = "캔버스 정보")
        public record CanvasInfo(
                        @Schema(description = "너비", example = "1200") Integer width,

                        @Schema(description = "높이", example = "800") Integer height,

                        @Schema(description = "좌석 반지름", example = "10") Integer seatRadius,

                        @Schema(description = "행 간격", example = "28") Integer rowGap,

                        @Schema(description = "열 간격", example = "28") Integer columnGap) {
        }

        /**
         * 구역 정보
         */
        @Builder
        @Schema(description = "구역 정보")
        public record SectionInfo(
                        @Schema(description = "구역 ID", example = "A") String sectionId,

                        @Schema(description = "구역명", example = "A구역") String name,

                        @Schema(description = "가격", example = "120000") Integer price,

                        @Schema(description = "색상", example = "#FF6B6B") String color) {
        }
}