package com.moa2.api.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 좌석 선점 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "좌석 선점 응답")
public class SeatLockResponse {

    @Schema(description = "선점 성공 여부", example = "true")
    @JsonProperty("isSuccess")
    private boolean isSuccess;

    @Schema(description = "선점 만료 시각 (ISO_LOCAL_DATE_TIME)", example = "2026-01-21T10:10:00")
    private LocalDateTime expiresAt;
}

