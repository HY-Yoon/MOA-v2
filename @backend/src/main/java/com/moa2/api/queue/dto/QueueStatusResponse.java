package com.moa2.api.queue.dto;

import com.moa2.global.model.QueueStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 대기열 상태 조회 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "대기열 상태 응답")
public class QueueStatusResponse {

    @Schema(description = "대기열 상태", example = "WAITING", allowableValues = {"WAITING", "READY", "EXPIRED", "COMPLETED"})
    private QueueStatus status;

    @Schema(description = "내 앞 대기 인원 수 (WAITING 상태일 때만)", example = "42")
    private Long position;

    @Schema(description = "예상 대기 시간(초) (WAITING 상태일 때만)", example = "84")
    private Long estimatedWaitTime;

    @Schema(description = "입장 가능 시간 (READY 상태일 때만)", example = "2026-01-20T10:10:00")
    private LocalDateTime activeUntil;

    /**
     * WAITING 상태 응답 생성
     */
    public static QueueStatusResponse waiting(Long position) {
        Long estimatedWaitTime = position * 2; // 예상 대기 시간 = 대기 인원 * 2초
        
        return QueueStatusResponse.builder()
                .status(QueueStatus.WAITING)
                .position(position)
                .estimatedWaitTime(estimatedWaitTime)
                .build();
    }

    /**
     * READY 상태 응답 생성
     */
    public static QueueStatusResponse ready(LocalDateTime activeUntil) {
        return QueueStatusResponse.builder()
                .status(QueueStatus.READY)
                .position(0L)
                .activeUntil(activeUntil)
                .build();
    }

    /**
     * EXPIRED 상태 응답 생성
     */
    public static QueueStatusResponse expired() {
        return QueueStatusResponse.builder()
                .status(QueueStatus.EXPIRED)
                .build();
    }

    /**
     * COMPLETED 상태 응답 생성
     */
    public static QueueStatusResponse completed() {
        return QueueStatusResponse.builder()
                .status(QueueStatus.COMPLETED)
                .build();
    }
}
