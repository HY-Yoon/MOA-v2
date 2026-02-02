package com.moa2.api.queue.controller.docs;

import com.moa2.api.queue.dto.QueueDto;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "대기열 API", description = "공연 예매 대기열 관리 API")
public interface QueueControllerDocs {
    @Operation(summary = "대기열 진입", description = """
            공연 예매 대기열에 진입합니다.
            
            **인증 방식:** Cookie (accessToken)
            
            **처리 과정:**
            1. 이미 대기열에 등록되어 있는지 확인
            2. 없으면 새로 등록, 있으면 기존 정보 반환
            3. 내 앞 대기 인원 수 계산
            
            **주의:** 같은 회차에 중복 진입은 불가능.
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대기열 진입 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "성공 응답", value = """
                    {
                      "success": true,
                      "data": {
                        "queueId": 5012,
                        "position": 150,
                        "estimatedWaitTime": 300
                      },
                      "message": null
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 - 존재하지 않는 스케줄", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "에러 응답", value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "존재하지 않는 스케줄입니다."
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 - 로그인 필요")
    })
    ResponseEntity<ApiResponse<QueueDto.EnterResponse>> enterQueue(
            // ▼ [중요] 기존 코드의 Swagger 전용 설정을 여기에 붙여넣습니다.
            // Spring의 @RequestBody가 아니라 Swagger 패키지의 어노테이션입니다.
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "대기열 진입 요청",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueueDto.EnterRequest.class),
                            examples = @ExampleObject(name = "요청 예시", value = """
                                    {
                                      "scheduleId": 7
                                    }
                                    """)
                    )
            )
            QueueDto.EnterRequest request
    );

    @Operation(summary = "대기 상태 조회", description = """
            내 대기열 상태를 조회합니다. 프론트엔드에서 3초마다 호출하여 상태를 확인합니다.
            
            **인증 방식:** Cookie (accessToken)
            
            **상태 종류:**
            - `WAITING`: 대기 중 (position 포함)
            - `READY`: 입장 가능 (activeUntil 포함)
            - `EXPIRED`: 시간 초과로 만료됨
            - `COMPLETED`: 결제 완료로 대기열 졸업
            
            **폴링 주기:** 3초 권장
            """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 - WAITING 상태", content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(name = "대기 중 (WAITING)", value = """
                            {
                              "success": true,
                              "data": {
                                "status": "WAITING",
                                "position": 0,
                                "estimatedWaitTime": 0,
                                "totalWaiting": 1,
                                "retryAfter": 3,
                                "activeUntil": null
                              },
                              "message": null
                            }
                            """),
                    @ExampleObject(name = "입장 가능 (READY)", value = """
                            {
                              "success": true,
                              "data": {
                                "status": "READY",
                                "position": 0,
                                "estimatedWaitTime": null,
                                "totalWaiting": null,
                                "retryAfter": null,
                                "activeUntil": "2026-01-28T09:43:49"
                              },
                              "message": null
                            }
                            """),
                    @ExampleObject(name = "만료됨 (EXPIRED)", value = """
                            {
                              "success": true,
                              "data": {
                                "status": "EXPIRED"
                              },
                              "message": null
                            }
                            """)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 - 대기열 정보 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "에러 응답", value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "대기열 정보를 찾을 수 없습니다. 먼저 대기열에 진입해주세요."
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 - 로그인 필요")
    })
    ResponseEntity<ApiResponse<QueueDto.StatusResponse>> getQueueStatus(
            @Parameter(description = "스케줄 ID", required = true, example = "100")
            Long scheduleId
    );
}