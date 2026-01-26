package com.moa2.api.queue.dto;

import com.moa2.global.model.QueueStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 대기열 관련 DTO 모음 (Record 변환 완료)
 */
public class QueueDto {

    /**
     * 대기열 진입 요청 DTO
     * (필드가 1개라 Builder 없이 생성자 방식 사용 추천)
     */
    @Schema(description = "대기열 진입 요청")
    public record EnterRequest(
            @NotNull(message = "스케줄 ID는 필수입니다")
            @Schema(description = "공연 회차 ID", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
            Long scheduleId
    ) {}

    /**
     * 대기열 진입 응답 DTO
     */
    @Builder
    @Schema(description = "대기열 진입 응답")
    public record EnterResponse(
            @Schema(description = "대기열 ID", example = "5012")
            Long queueId,

            @Schema(description = "내 앞 대기 인원 수", example = "150")
            Long position,

            @Schema(description = "예상 대기 시간(초)", example = "300")
            Long estimatedWaitTime
    ) {
        // static 생성 메서드는 record 중괄호 {} 안에 그대로 넣으면 됩니다.
        public static EnterResponse of(Long queueId, Long position) {
            long estimatedWaitTime = position * 2; // 계산 로직

            return EnterResponse.builder()
                    .queueId(queueId)
                    .position(position)
                    .estimatedWaitTime(estimatedWaitTime)
                    .build();
        }
    }

    /**
     * 대기열 상태 조회 응답 DTO
     */
    @Builder
    @Schema(description = "대기열 상태 응답")
    public record StatusResponse(
            @Schema(description = "대기열 상태", example = "WAITING", allowableValues = {"WAITING", "READY", "EXPIRED", "COMPLETED"})
            QueueStatus status,

            @Schema(description = "내 앞 대기 인원 수 (WAITING 상태일 때만)", example = "42")
            Long position,

            @Schema(description = "예상 대기 시간(초) (WAITING 상태일 때만)", example = "84")
            Long estimatedWaitTime,

            @Schema(description = "입장 가능 시간 (READY 상태일 때만)", example = "2026-01-20T10:10:00")
            LocalDateTime activeUntil
    ) {
        /**
         * WAITING 상태 응답 생성
         */
        public static StatusResponse waiting(Long position) {
            long estimatedWaitTime = position * 2;

            return StatusResponse.builder()
                    .status(QueueStatus.WAITING)
                    .position(position)
                    .estimatedWaitTime(estimatedWaitTime)
                    .build();
        }

        /**
         * READY 상태 응답 생성
         */
        public static StatusResponse ready(LocalDateTime activeUntil) {
            return StatusResponse.builder()
                    .status(QueueStatus.READY)
                    .position(0L) // 대기인원 0명
                    .activeUntil(activeUntil)
                    .build();
        }

        /**
         * EXPIRED 상태 응답 생성
         */
        public static StatusResponse expired() {
            return StatusResponse.builder()
                    .status(QueueStatus.EXPIRED)
                    .build();
        }

        /**
         * COMPLETED 상태 응답 생성
         */
        public static StatusResponse completed() {
            return StatusResponse.builder()
                    .status(QueueStatus.COMPLETED)
                    .build();
        }
    }
}