package com.moa2.api.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 좌석 선점 해제 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "좌석 선점 해제 응답")
public class SeatUnlockResponse {

    @Schema(description = "선점 해제 성공 여부", example = "true")
    @JsonProperty("isSuccess")
    private boolean isSuccess;
}

