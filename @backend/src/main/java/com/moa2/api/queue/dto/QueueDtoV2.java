package com.moa2.api.queue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * V2: Redis 기반 대기열 DTO
 */
public class QueueDtoV2 {

    /**
     * 대기열 진입 요청 DTO
     */
    @Schema(description = "V2 대기열 진입 요청")
    public record EnterRequest(
            @NotNull(message = "스케줄 ID는 필수입니다") @Schema(description = "공연 회차 ID", example = "7", requiredMode = Schema.RequiredMode.REQUIRED) Long scheduleId) {
    }

    /**
     * 대기열 진입 응답 DTO
     */
    @Builder
    @Schema(description = "V2 대기열 진입 응답")
    public record EnterResponse(
            @Schema(description = "대기열 등록 여부", example = "true") boolean success,

            @Schema(description = "내 앞 대기 인원 수 (0이면 즉시 입장 가능)", example = "150") Long position,

            @Schema(description = "예상 대기 시간(초)", example = "2") Long estimatedWaitTimeSeconds,

            @Schema(description = "토큰 (position=0일 때만 발급)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") String token,

            @Schema(description = "메시지", example = "대기열에 등록되었습니다") String message) {
        /**
         * 대기 중 (position > 0)
         */
        public static EnterResponse waiting(Long position, Long estimatedWaitTimeSeconds) {
            return EnterResponse.builder()
                    .success(true)
                    .position(position)
                    .estimatedWaitTimeSeconds(estimatedWaitTimeSeconds)
                    .message("대기열에 등록되었습니다. 약 " + estimatedWaitTimeSeconds + "초 후 입장 가능합니다.")
                    .build();
        }

        /**
         * 이미 대기 중인 경우
         */
        public static EnterResponse alreadyWaiting(Long position, Long estimatedWaitTimeSeconds) {
            return EnterResponse.builder()
                    .success(true)
                    .position(position)
                    .estimatedWaitTimeSeconds(estimatedWaitTimeSeconds)
                    .message("이미 대기열에 등록되어 있습니다. 현재 " + position + "번째입니다.")
                    .build();
        }

        /**
         * 즉시 입장 가능 (토큰 발급)
         */
        public static EnterResponse ready(String token) {
            return EnterResponse.builder()
                    .success(true)
                    .position(0L)
                    .estimatedWaitTimeSeconds(0L)
                    .token(token)
                    .message("입장 가능합니다. 토큰이 발급되었습니다.")
                    .build();
        }
    }

    /**
     * 대기열 상태 조회 응답 DTO
     */
    @Builder
    @Schema(description = "V2 대기열 상태 응답")
    public record StatusResponse(
            @Schema(description = "대기열 상태", example = "WAITING", allowableValues = {
                    "WAITING", "READY", "EXPIRED" }) String status,

            @Schema(description = "내 앞 대기 인원 수 (WAITING 상태일 때)", example = "42") Long position,

            @Schema(description = "예상 대기 시간(초) (WAITING 상태일 때)", example = "1") Long estimatedWaitTimeSeconds,

            @Schema(description = "재시도 간격(초) - 폴링 주기", example = "3") Long retryAfterSeconds,

            @Schema(description = "입장 토큰 (READY 상태일 때)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") String token,

            @Schema(description = "메시지", example = "현재 42번째입니다") String message) {
        /**
         * WAITING 상태 응답
         */
        public static StatusResponse waiting(Long position, Long estimatedWaitTimeSeconds) {
            // 대기 인원에 따라 폴링 주기 조정
            long retryAfter = position > 100 ? 5L : 2L;

            return StatusResponse.builder()
                    .status("WAITING")
                    .position(position)
                    .estimatedWaitTimeSeconds(estimatedWaitTimeSeconds)
                    .retryAfterSeconds(retryAfter)
                    .message("현재 " + position + "번째입니다. 잠시만 기다려주세요.")
                    .build();
        }

        /**
         * READY 상태 응답 (토큰 발급됨)
         */
        public static StatusResponse ready(String token) {
            return StatusResponse.builder()
                    .status("READY")
                    .position(0L)
                    .estimatedWaitTimeSeconds(0L)
                    .token(token)
                    .message("입장 가능합니다. 5분 내에 예매를 진행해주세요.")
                    .build();
        }

        /**
         * EXPIRED 상태 응답 (토큰 만료)
         */
        public static StatusResponse expired() {
            return StatusResponse.builder()
                    .status("EXPIRED")
                    .message("대기 시간이 만료되었습니다. 다시 대기열에 진입해주세요.")
                    .build();
        }

        /**
         * NOT_FOUND 상태 응답 (대기열에 없음)
         */
        public static StatusResponse notFound() {
            return StatusResponse.builder()
                    .status("NOT_FOUND")
                    .message("대기열에 등록되어 있지 않습니다. 먼저 대기열에 진입해주세요.")
                    .build();
        }
    }
}
