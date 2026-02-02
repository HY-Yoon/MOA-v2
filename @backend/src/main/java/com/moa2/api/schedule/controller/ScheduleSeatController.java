package com.moa2.api.schedule.controller;

import com.moa2.api.schedule.controller.docs.ScheduleSeatControllerDocs;
import com.moa2.api.schedule.dto.ScheduleDto;
import com.moa2.api.schedule.service.ScheduleSeatService;
import com.moa2.global.dto.ApiResponse;
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
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleSeatController implements ScheduleSeatControllerDocs {

    private final ScheduleSeatService scheduleSeatService;

    @Override
    @GetMapping("/{scheduleId}/seats")
    public ResponseEntity<ApiResponse<ScheduleDto.SeatsResponse>> getScheduleSeats(@PathVariable Long scheduleId) {
        try {
            ScheduleDto.SeatsResponse result = scheduleSeatService.getScheduleSeats(scheduleId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            log.warn("좌석 상태 조회 실패: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Override
    @GetMapping("/{scheduleId}/seatmap")
    public ResponseEntity<ApiResponse<ScheduleDto.SeatMapResponse>> getSeatMap(@PathVariable Long scheduleId) {
        try {
            ScheduleDto.SeatMapResponse result = scheduleSeatService.getSeatMap(scheduleId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            log.warn("좌석 배치도 조회 실패: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
