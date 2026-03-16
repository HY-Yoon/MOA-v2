package com.moa2.api.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * (local/test 전용) k6/통합 테스트를 위한 대기열 READY 강제 세팅 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대기열 READY 강제 세팅 요청")
public class TestQueueReadyRequest {

    @Email
    @NotBlank
    @Schema(description = "사용자 이메일", example = "k6-vu1@moa2.test")
    private String email;

    @NotBlank
    @Schema(description = "소셜 제공자", example = "GOOGLE")
    private String provider;

    @NotNull
    @Schema(description = "스케줄 ID", example = "1")
    private Long scheduleId;

    @Schema(description = "READY 세션 TTL(분)", example = "10")
    private Integer ttlMinutes;
}

