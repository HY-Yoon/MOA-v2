package com.moa2.api.queue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 대기열 진입 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "대기열 진입 응답")
public class QueueEnterResponse {

    @Schema(description = "대기열 ID", example = "5012")
    private Long queueId;

    @Schema(description = "내 앞 대기 인원 수", example = "150")
    private Long position;

    @Schema(description = "예상 대기 시간(초)", example = "300")
    private Long estimatedWaitTime;

    public static QueueEnterResponse of(Long queueId, Long position) {
        // 예상 대기 시간 = 대기 인원 * 2초 (예시)
        Long estimatedWaitTime = position * 2;
        
        return QueueEnterResponse.builder()
                .queueId(queueId)
                .position(position)
                .estimatedWaitTime(estimatedWaitTime)
                .build();
    }
}
