package com.moa2.api.reservation.controller.docs;

import com.moa2.api.reservation.dto.ReservationDtoV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/**
 * V2: Redis 기반 예매 API 문서
 */
@Tag(name = "예매 API V2", description = "Redis 기반 공연 예매 API (V2)")
public interface ReservationControllerV2Docs {

  @Operation(summary = "좌석 예매 (V2)", description = """
      Redis 분산 락 기반으로 좌석을 선점하고 예매를 생성합니다.

      **V1 대비 개선점:**
      - Redisson 분산 락으로 빠른 동시성 제어
      - 토큰 기반 입장 제어 (대기열 통과자만 예매 가능)

      **필수 헤더:**
      - `X-Queue-Token`: 대기열 통과 시 발급받은 토큰

      **인증 방식:** Cookie (accessToken)

      **처리 과정:**
      1. 토큰 검증 (소유자/스케줄 확인)
      2. 좌석별 분산 락 획득
      3. 좌석 상태 확인 및 선점
      4. 예약 생성
      5. 토큰 소진 (재사용 방지)
      """)
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "예매 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": true,
            "data": {
              "reservationId": 1234,
              "reservationNumber": "RES-20260203-A1B2C3",
              "orderId": "MOA-abc123def456ghi789jk",
              "seatCount": 2,
              "totalAmount": 100000,
              "paymentDeadline": "2026-02-03T11:00:00",
              "message": "좌석 선점 완료! 11:00:00까지 결제해주세요."
            }
          }
          """))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "예매 실패", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "좌석 충돌 (SEAT_CONFLICT)", value = """
              {
                "success": false,
                "data": {
                  "code": "SEAT_CONFLICT",
                  "conflictSeatIds": ["A-4", "B-7"]
                },
                "message": "이미 선점된 좌석이 있습니다: [A-4, B-7]"
              }
              """),
          @ExampleObject(name = "토큰 만료 (QUEUE_EXPIRED)", value = """
              {
                "success": false,
                "data": {
                  "code": "QUEUE_EXPIRED"
                },
                "message": "토큰이 만료되었거나 유효하지 않습니다"
              }
              """),
          @ExampleObject(name = "잘못된 요청 (BAD_REQUEST)", value = """
              {
                "success": false,
                "data": {
                  "code": "BAD_REQUEST"
                },
                "message": "좌석을 찾을 수 없습니다: 9999"
              }
              """)
      }))
  })
  ResponseEntity<?> reserve(
      @Parameter(description = "대기열 통과 토큰", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") String token,

      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "예매 요청", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservationDtoV2.ReserveRequest.class), examples = @ExampleObject(value = """
          {
            "scheduleId": 7,
            "scheduleSeatIds": [101, 102]
          }
          """))) ReservationDtoV2.ReserveRequest request);
}
