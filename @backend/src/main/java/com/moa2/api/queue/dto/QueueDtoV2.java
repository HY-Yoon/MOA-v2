package com.moa2.api.queue.dto;

import com.moa2.global.model.QueueStatus;
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
     * - WAITING: position, estimatedWaitTimeSeconds, retryAfterSeconds 포함
     * - READY: token 포함
     */
    @Builder
    @Schema(description = "V2 대기열 진입 응답")
    public record EnterResponse(
            @Schema(description = "대기열 상태", example = "WAITING", allowableValues = {
                    "WAITING", "READY" }) QueueStatus status,

            @Schema(description = "내 앞 대기 인원 수 (WAITING 상태일 때)", example = "150") Long position,

            @Schema(description = "전체 대기 인원 수 (WAITING 상태일 때)", example = "523") Long totalWaiting,

            @Schema(description = "예상 대기 시간(초) (WAITING 상태일 때)", example = "3") Long estimatedWaitTimeSeconds,

            @Schema(description = "폴링 권장 간격(초) (WAITING: totalWaiting 10만↑이면 10초, 기본 3초)", example = "3") Long retryAfterSeconds,

            @Schema(description = "입장 토큰 (READY 상태일 때만)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") String token,

            @Schema(description = "메시지", example = "대기열에 등록되었습니다") String message) {

        /** 대기 중 */
        public static EnterResponse waiting(Long position, Long totalWaiting, Long estimatedWaitTimeSeconds) {
            // totalWaiting 10만 이상이면 10초, 기본 3초
            long retryAfter = (totalWaiting != null && totalWaiting >= 100_000) ? 10L : 3L;
            return EnterResponse.builder()
                    .status(QueueStatus.WAITING)
                    .position(position)
                    .totalWaiting(totalWaiting)
                    .estimatedWaitTimeSeconds(estimatedWaitTimeSeconds)
                    .retryAfterSeconds(retryAfter)
                    .message("대기열에 등록되었습니다. 약 " + estimatedWaitTimeSeconds + "초 후 입장 가능합니다.")
                    .build();
        }

        /** 즉시 입장 가능 (토큰 발급) */
        public static EnterResponse ready(String token) {
            return EnterResponse.builder()
                    .status(QueueStatus.READY)
                    .position(0L)
                    .totalWaiting(0L)
                    .estimatedWaitTimeSeconds(0L)
                    .retryAfterSeconds(0L)
                    .token(token)
                    .message("입장 가능합니다. 토큰이 발급되었습니다.")
                    .build();
        }

        /** 쿠키로 토큰을 발급했으므로 Body의 token 필드를 null로 마스킹 */
        public EnterResponse withTokenMasked() {
            return EnterResponse.builder()
                    .status(this.status())
                    .position(this.position())
                    .totalWaiting(this.totalWaiting())
                    .estimatedWaitTimeSeconds(this.estimatedWaitTimeSeconds())
                    .retryAfterSeconds(this.retryAfterSeconds())
                    .token(null)  // HttpOnly 쿠키로 전달하므로 Body에는 노출 안함
                    .message(this.message())
                    .build();
        }
    }

    /**
     * 대기열 상태 조회 응답 DTO
     * - 모든 상태에서 동일한 구조 반환 (미사용 필드는 null)
     * - WAITING: position, totalWaiting, estimatedWaitTimeSeconds,
     * retryAfterSeconds 채움
     * - READY: token 채움
     * - EXPIRED: message만 채움
     * - NOT_FOUND: message만 채움
     */
    @Builder
    @Schema(description = "V2 대기열 상태 응답")
    public record StatusResponse(
            @Schema(description = "대기열 상태", example = "WAITING", allowableValues = {
                    "WAITING", "READY", "EXPIRED", "NOT_FOUND" }) QueueStatus status,

            @Schema(description = "내 앞 대기 인원 수 (WAITING 상태일 때)", example = "42") Long position,

            @Schema(description = "전체 대기 인원 수 (WAITING 상태일 때)", example = "523") Long totalWaiting,

            @Schema(description = "예상 대기 시간(초) (WAITING 상태일 때)", example = "84") Long estimatedWaitTimeSeconds,

            @Schema(description = "폴링 권장 간격(초) (WAITING: totalWaiting 10만↑이면 10초, 기본 3초)", example = "3") Long retryAfterSeconds,

            @Schema(description = "입장 토큰 (READY 상태일 때만)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") String token,

            @Schema(description = "메시지", example = "현재 42번째입니다") String message) {

        /** WAITING 상태 */
        public static StatusResponse waiting(Long position, Long totalWaiting, Long estimatedWaitTimeSeconds) {
            long retryAfter = (totalWaiting != null && totalWaiting >= 100_000) ? 10L : 3L;
            return StatusResponse.builder()
                    .status(QueueStatus.WAITING)
                    .position(position)
                    .totalWaiting(totalWaiting)
                    .estimatedWaitTimeSeconds(estimatedWaitTimeSeconds)
                    .retryAfterSeconds(retryAfter)
                    .message("현재 " + position + "번째입니다. 잠시만 기다려주세요.")
                    .build();
        }

        /** READY 상태 (토큰 발급됨) */
        public static StatusResponse ready(String token) {
            return StatusResponse.builder()
                    .status(QueueStatus.READY)
                    .position(0L)
                    .totalWaiting(0L)
                    .estimatedWaitTimeSeconds(0L)
                    .retryAfterSeconds(0L)
                    .token(token)
                    .message("입장 가능합니다. 5분 내에 예매를 진행해주세요.")
                    .build();
        }

        /** 쿠키로 토큰을 발급했으므로 Body의 token 필드를 null로 마스킹 */
        public StatusResponse withTokenMasked() {
            return StatusResponse.builder()
                    .status(this.status())
                    .position(this.position())
                    .totalWaiting(this.totalWaiting())
                    .estimatedWaitTimeSeconds(this.estimatedWaitTimeSeconds())
                    .retryAfterSeconds(this.retryAfterSeconds())
                    .token(null)  // HttpOnly 쿠키로 전달하므로 Body에는 노출 안함
                    .message(this.message())
                    .build();
        }

        /** EXPIRED 상태 */
        public static StatusResponse expired() {
            return StatusResponse.builder()
                    .status(QueueStatus.EXPIRED)
                    .message("대기 시간이 만료되었습니다. 다시 대기열에 진입해주세요.")
                    .build();
        }

        /** NOT_FOUND 상태 (대기열 미진입) */
        public static StatusResponse notFound() {
            return StatusResponse.builder()
                    .status(QueueStatus.NOT_FOUND)
                    .message("대기열에 등록되어 있지 않습니다. 먼저 대기열에 진입해주세요.")
                    .build();
        }
    }

    @Schema(description = "V2 디버그 WAITING 시나리오 요청")
    public record DebugWaitingScenarioRequest(
            @NotNull(message = "스케줄 ID는 필수입니다")
            @Schema(description = "공연 회차 ID", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
            Long scheduleId,

            @Schema(description = "내 앞에 둘 가짜 대기 인원 수", example = "3", defaultValue = "3")
            Long usersAhead
    ) {
        public long normalizedUsersAhead() {
            if (usersAhead == null || usersAhead < 1) {
                return 3L;
            }
            return usersAhead;
        }
    }

    @Schema(description = "V2 디버그 READY 시나리오 요청")
    public record DebugReadyScenarioRequest(
            @NotNull(message = "스케줄 ID는 필수입니다")
            @Schema(description = "공연 회차 ID", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
            Long scheduleId
    ) {}

    @Schema(description = "V2 디버그 시나리오 초기화 요청")
    public record DebugResetScenarioRequest(
            @NotNull(message = "스케줄 ID는 필수입니다")
            @Schema(description = "공연 회차 ID", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
            Long scheduleId
    ) {}

    @Builder
    @Schema(description = "V2 디버그 시나리오 응답")
    public record DebugScenarioResponse(
            @Schema(description = "처리 메시지", example = "WAITING 시나리오 구성 완료")
            String message,

            @Schema(description = "스케줄 ID", example = "7")
            Long scheduleId,

            @Schema(description = "로그인 사용자 ID", example = "1")
            Long userId,

            @Schema(description = "예상 내 순번 (WAITING 시나리오일 때)", example = "4")
            Long position,

            @Schema(description = "예상 대기열 총원 (WAITING 시나리오일 때)", example = "4")
            Long totalWaiting
    ) {
        public static DebugScenarioResponse of(
                String message,
                Long scheduleId,
                Long userId,
                Long position,
                Long totalWaiting
        ) {
            return DebugScenarioResponse.builder()
                    .message(message)
                    .scheduleId(scheduleId)
                    .userId(userId)
                    .position(position)
                    .totalWaiting(totalWaiting)
                    .build();
        }
    }
}
