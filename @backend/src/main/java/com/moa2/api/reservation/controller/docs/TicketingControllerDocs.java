package com.moa2.api.reservation.controller.docs;

import com.moa2.api.reservation.dto.ReservationDtoV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 티켓팅 API 문서
 */
@Tag(name = "티켓팅 API V2", description = "예매 진행 플로우 및 주문 처리 API (V2)")
public interface TicketingControllerDocs {

  @Operation(summary = "좌석 선점 (V2)", description = """
      Redis 분산 락 기반으로 좌석을 선점합니다. DB Write 없이 즉시 200 응답합니다.

      **처리 과정:**
      1. 토큰 검증 (소유자/스케줄 확인)
      2. 좌석별 분산 락 획득
      3. Redis에 좌석 선점 상태 저장 (TTL 5분)
      4. 토큰 소진 (재사용 방지)
      5. 200 OK + 선점 정보 응답

      **인증 방식:** Cookie (QUEUE-TOKEN HttpOnly 쿠키 자동 전송 / accessToken)
      """)
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좌석 선점 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": true,
            "data": {
              "seatCount": 2,
              "remainingSeconds": 300,
              "totalAmount": 132000,
              "message": "좌석 선점 완료! 5분 내에 결제를 진행해주세요."
            },
            "message": null
          }
          """))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "선점 실패", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "좌석 충돌 (SEAT_CONFLICT)", value = """
              {
                "success": false,
                "data": {
                  "code": "SEAT_CONFLICT",
                  "conflictSeatIds": ["101", "102"]
                },
                "message": "이미 선점된 좌석이 있습니다: [101, 102]"
              }
              """),
          @ExampleObject(name = "토큰 만료 (QUEUE_EXPIRED)", value = """
              {
                "success": false,
                "data": { "code": "QUEUE_EXPIRED" },
                "message": "토큰이 만료되었거나 유효하지 않습니다"
              }
              """)
      }))
  })
  ResponseEntity<?> reserve(
      jakarta.servlet.http.HttpServletRequest httpRequest,
      jakarta.servlet.http.HttpServletResponse httpResponse,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "좌석 선점 요청", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservationDtoV2.ReserveRequest.class))) ReservationDtoV2.ReserveRequest request);

  @Operation(summary = "주문 미리보기 (V2)", description = """
      결제 페이지 진입 시 필요한 주문 상세 정보를 조회합니다.
      선점한 좌석 상세 내역, 결제 금액 합계, 잔여 결제 시간, 예약자 정보를 반환합니다.

      **인증 방식:** Cookie (accessToken)
      """)
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "미리보기 조회 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "success": true,
            "data": {
              "title": "뮤지컬 <ANNE> 10th Anniversary",
              "venueName": "대학로 자유극장",
              "showDate": "2025-12-09",
              "showTime": "19:30",
              "bookingFee": 4000,
              "ticketAmount": 132000,
              "totalAmount": 136000,
              "paymentDeadline": "2026-03-06T23:55:00",
              "seats": [
                {
                  "scheduleSeatId": 3701,
                  "gradeName": "R석",
                  "seatNumber": "B열 17번",
                  "price": 66000
                },
                {
                  "scheduleSeatId": 3702,
                  "gradeName": "R석",
                  "seatNumber": "B열 18번",
                  "price": 66000
                }
              ],
              "defaultBooker": {
                "name": "홍길동",
                "email": "hong@example.com",
                "phone": "010-1234-5678"
              }
            },
            "message": null
          }
          """))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "조회 실패", content = @Content(mediaType = "application/json", examples = {
          @ExampleObject(name = "선점 만료", value = """
              {
                "success": false,
                "data": { "code": "BAD_REQUEST" },
                "message": "좌석 선점 시간이 만료되었습니다. 좌석 ID: 3701"
              }
              """)
      }))
  })
  ResponseEntity<?> getPreview(
      @Parameter(description = "공연 회차 ID", required = true, example = "21") Long scheduleId,
      @Parameter(description = "선점한 좌석 ID 목록", required = true, example = "3701,3702") java.util.List<Long> scheduleSeatIds);

}
