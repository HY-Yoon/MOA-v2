package com.moa2.api.schedule.controller.docs;

import com.moa2.api.schedule.dto.ScheduleDto;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "회차 좌석 API", description = "회차별 좌석 배치도 조회 API")
public interface ScheduleSeatControllerDocs {
        @Operation(summary = "좌석 좌표 + 상태 조회", description = """
                        특정 회차(scheduleId)의 좌석 좌표와 상태를 조회합니다. (상세 좌석 선택 화면용)

                        **응답 정보:**
                        - `maxSelectable`: 한번 예매시 최대 선택 가능 좌석 수 (6석)
                        - `seats`: 좌석 목록 (seatId, sectionId, row, number, x, y, status)

                        **주의:** 대기열 상태가 READY인 사용자만 조회 가능하며, 만료되면 403을 반환합니다.
                        """)
        @Parameter(name = "X-Queue-Token", description = "대기열 입장 토큰 (V2의 경우 필수)", in = ParameterIn.HEADER, required = false)
        ResponseEntity<ApiResponse<ScheduleDto.SeatsResponse>> getScheduleSeats(
                        @Parameter(description = "스케줄 ID", required = true, example = "10") @PathVariable Long scheduleId);

        @Operation(summary = "좌석 배치도 조회 (canvas + sections)", description = """
                        특정 회차(scheduleId)의 좌석 배치도 정보를 조회합니다. (구역 선택 화면용)

                        **응답 정보:**
                        - `canvas`: 배치도 캔버스 정보 (width, height, seatRadius)
                        - `sections`: 구역 목록 (sectionId, name, price, color)

                        **주의:** 대기열 상태가 READY인 사용자만 조회 가능하며, 만료되면 403을 반환합니다.
                        """)
        @Parameter(name = "X-Queue-Token", description = "대기열 입장 토큰 (V2의 경우 필수)", in = ParameterIn.HEADER, required = false)
        ResponseEntity<ApiResponse<ScheduleDto.SeatMapResponse>> getSeatMap(
                        @Parameter(description = "스케줄 ID", required = true, example = "10") @PathVariable Long scheduleId);
}
