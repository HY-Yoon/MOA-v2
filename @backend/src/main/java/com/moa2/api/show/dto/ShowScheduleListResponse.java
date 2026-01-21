package com.moa2.api.show.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 날짜별 회차 조회 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "공연 회차 조회 응답")
public class ShowScheduleListResponse {

    @Schema(description = "스케줄 ID", example = "100")
    private Long scheduleId;

    @Schema(description = "공연 날짜", example = "2026-01-25")
    private LocalDate date;

    @Schema(description = "공연 시간", example = "14:00")
    private LocalTime time;

    @Schema(description = "매진 여부", example = "false")
    private boolean isSoldOut;
}

