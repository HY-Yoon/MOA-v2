package com.moa2.api.schedule.controller.docs;

import com.moa2.api.schedule.dto.ScheduleDto;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "회차 좌석 선점 API", description = "회차별 좌석 선점(LOCK) 및 해제(UNLOCK) API")
public interface ScheduleSeatLockControllerDocs {

    @Operation(summary = "좌석 선점 (LOCK)", description = """
            '선택하기'를 누르는 순간 특정 회차(scheduleId)의 좌석을 5분간 선점(LOCK)합니다.
            
            **규칙:**
            - 요청 좌석이 모두 `AVAILABLE`일 때만 성공
            - 성공 시 `isSuccess: true`와 `expiresAt` 반환
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "선점 성공", content = @Content(examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "isSuccess": true,
                        "expiresAt": "2026-02-01T20:30:00"
                      },
                      "message": null
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "선점 실패 - 이미 선택된 좌석")
    })
    ResponseEntity<ApiResponse<ScheduleDto.SeatLockResponse>> lockSeats(
            @Parameter(description = "스케줄 ID", required = true, example = "10")
            @PathVariable Long scheduleId,

            @RequestBody(description = "선점 요청 정보", required = true, content = @Content(schema = @Schema(implementation = ScheduleDto.SeatLockRequest.class)))
            @Valid ScheduleDto.SeatLockRequest request
    );

    @Operation(summary = "좌석 선점 해제 (UNLOCK)", description = """
            사용자가 선점한 좌석을 즉시 해제(UNLOCK)합니다.
            
            **규칙:**
            - 내 좌석(`LOCKED` + `lockedByUserId`)만 해제 가능
            - 이미 해제된 경우도 성공(`true`) 처리 (멱등성)
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "해제 성공", content = @Content(examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "isSuccess": true
                      },
                      "message": null
                    }
                    """)))
    })
    ResponseEntity<ApiResponse<ScheduleDto.SeatUnlockResponse>> unlockSeats(
            @Parameter(description = "스케줄 ID", required = true, example = "10")
            @PathVariable Long scheduleId,

            @RequestBody(description = "해제 요청 정보", required = true, content = @Content(schema = @Schema(implementation = ScheduleDto.SeatUnlockRequest.class)))
            @Valid ScheduleDto.SeatUnlockRequest request
    );
}