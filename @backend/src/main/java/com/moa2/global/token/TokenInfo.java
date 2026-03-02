package com.moa2.global.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * V2: Queue 토큰 정보
 * - 대기열 통과 후 발급받는 입장 토큰
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenInfo {

    /**
     * 토큰 소유자 ID
     */
    private Long userId;

    /**
     * 예매 대상 스케줄 ID
     */
    private Long scheduleId;

    /**
     * 토큰 사용 여부 (재사용 방지)
     */
    private boolean used;
}
