package com.moa2.api.schedule.controller;

import com.moa2.api.schedule.dto.ScheduleDto;
import com.moa2.api.schedule.service.ScheduleSeatService;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회차 좌석 배치도 조회 컨트롤러
 * - 대기열 READY 상태 사용자만 접근 가능 (Interceptor에서 검증)
 */
@Slf4j
@Tag(name = "회차 좌석 API", description = "회차별 좌석 배치도 조회 API")
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleSeatController {

    private final ScheduleSeatService scheduleSeatService;

    @Operation(
            summary = "좌석 배치도 조회",
            description = "특정 회차(scheduleId)의 좌석 배치도와 좌석 상태를 조회합니다.\n\n" +
                    "**주의:** 대기열 상태가 READY인 사용자만 조회 가능하며, 만료되면 403을 반환합니다."
    )
    @GetMapping("/{scheduleId}/seats")
    public ResponseEntity<ApiResponse<ScheduleDto.SeatsResponse>> getScheduleSeats(
            @Parameter(description = "스케줄 ID", required = true) @PathVariable Long scheduleId
    ) {
        try {
            ScheduleDto.SeatsResponse result = scheduleSeatService.getScheduleSeats(scheduleId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            log.warn("좌석 배치도 조회 실패: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

