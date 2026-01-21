package com.moa2.api.schedule.dto;

import com.moa2.global.model.SeatStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 회차 좌석 배치도 조회 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "회차 좌석 배치도 응답")
public class ScheduleSeatsResponse {

    @Schema(description = "스케줄 ID", example = "100")
    private Long scheduleId;

    @Schema(description = "좌석 목록")
    private List<SeatInfo> seats;

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

