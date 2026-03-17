package com.moa2.api.test.dto;

import com.moa2.global.model.QueueStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * (local/test 전용) 대기열 READY 강제 세팅 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "대기열 READY 강제 세팅 응답")
public class TestQueueReadyResponse {

    @Schema(description = "Queue ID", example = "100")
    private Long queueId;

    @Schema(description = "스케줄 ID", example = "1")
    private Long scheduleId;

    @Schema(description = "대기열 상태", example = "READY")
    private QueueStatus status;

    @Schema(description = "READY 유효 시간(만료 시각)", example = "2026-01-22T10:10:00")
    private LocalDateTime activeUntil;
}

