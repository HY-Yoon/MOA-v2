package com.moa2.api.queue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대기열 진입 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대기열 진입 요청")
public class QueueEnterRequest {

    @NotNull(message = "스케줄 ID는 필수입니다")
    @Schema(description = "공연 회차 ID", example = "100", required = true)
    private Long scheduleId;
}
