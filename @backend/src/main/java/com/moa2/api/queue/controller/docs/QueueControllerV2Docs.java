package com.moa2.api.queue.controller.docs;

import com.moa2.api.queue.dto.QueueDtoV2;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/**
 * V2: Redis 기반 대기열 API 문서
 */
@Tag(name = "대기열 API V2", description = "Redis 기반 공연 예매 대기열 관리 API (V2)")
public interface QueueControllerV2Docs {

    @Operation(summary = "대기열 진입 (V2)", description = """
            Redis 기반 대기열에 진입합니다.

            **V1 대비 개선점:**
            - Redis Sorted Set으로 빠른 순위 조회
            - 즉시 입장 가능 시 토큰 발급
            - 새로고침해도 기존 토큰 유지

            **인증 방식:** Cookie (accessToken)

            **처리 과정:**
            1. 기존 토큰 확인 (새로고침 대응)
            2. 대기열에 등록 (없으면 새로 등록, 있으면 기존 위치 반환)
            3. 첫 번째 순서면 즉시 토큰 발급
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대기열 진입 성공", content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(name = "대기 중", value = """
                            {
                              "success": true,
                              "data": {
                                "success": true,
                                "position": 150,
                                "estimatedWaitTimeSeconds": 2,
                                "token": null,
                                "message": "대기열에 등록되었습니다. 약 2초 후 입장 가능합니다."
                              }
                            }
                            """),
                    @ExampleObject(name = "즉시 입장 가능", value = """
                            {
                              "success": true,
                              "data": {
                                "success": true,
                                "position": 0,
                                "estimatedWaitTimeSeconds": 0,
                                "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                "message": "입장 가능합니다. 토큰이 발급되었습니다."
                              }
                            }
                            """)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "존재하지 않는 스케줄입니다"
                    }
                    """)))
    })
    ResponseEntity<ApiResponse<QueueDtoV2.EnterResponse>> enterQueue(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "대기열 진입 요청", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = QueueDtoV2.EnterRequest.class), examples = @ExampleObject(value = """
                    {
                      "scheduleId": 7
                    }
                    """))) QueueDtoV2.EnterRequest request);

    @Operation(summary = "대기 상태 조회 (V2)", description = """
            대기열 상태를 조회합니다. 프론트엔드에서 폴링하여 상태를 확인합니다.

            **V1 대비 개선점:**
            - 토큰 우선 확인 (새로고침해도 토큰 유지)
            - 빠른 순위 조회 (Redis ZRANK)

            **상태 종류:**
            - `WAITING`: 대기 중 (position, estimatedWaitTimeSeconds 포함)
            - `READY`: 입장 가능 (token 포함)
            - `EXPIRED`: 토큰 만료됨 (다시 대기열 진입 필요)
            - `NOT_FOUND`: 대기열에 없음 (대기열 진입 필요)

            **폴링 주기:** retryAfterSeconds 참고 (2~5초)
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(name = "대기 중 (WAITING)", value = """
                            {
                              "success": true,
                              "data": {
                                "status": "WAITING",
                                "position": 42,
                                "estimatedWaitTimeSeconds": 1,
                                "retryAfterSeconds": 2,
                                "token": null,
                                "message": "현재 42번째입니다. 잠시만 기다려주세요."
                              }
                            }
                            """),
                    @ExampleObject(name = "입장 가능 (READY)", value = """
                            {
                              "success": true,
                              "data": {
                                "status": "READY",
                                "position": 0,
                                "estimatedWaitTimeSeconds": 0,
                                "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                "message": "입장 가능합니다. 5분 내에 예매를 진행해주세요."
                              }
                            }
                            """),
                    @ExampleObject(name = "만료됨 (EXPIRED)", value = """
                            {
                              "success": true,
                              "data": {
                                "status": "EXPIRED",
                                "message": "대기 시간이 만료되었습니다. 다시 대기열에 진입해주세요."
                              }
                            }
                            """)
            }))
    })
    ResponseEntity<ApiResponse<QueueDtoV2.StatusResponse>> getQueueStatus(
            @Parameter(description = "스케줄 ID", required = true, example = "7") Long scheduleId);
}
