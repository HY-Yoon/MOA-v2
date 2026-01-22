package com.moa2.api.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 좌석 선점 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "좌석 선점 요청")
public class SeatLockRequest {

    @NotEmpty(message = "seatIds는 최소 1개 이상 필요합니다")
    @Schema(description = "선점할 좌석 ID 목록", example = "[10, 11]")
    private List<Long> seatIds;
}

