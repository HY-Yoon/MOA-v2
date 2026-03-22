package com.moa2.api.payment.controller.docs;

import com.moa2.api.payment.dto.PaymentDto;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "결제 API", description = "토스페이먼츠 결제 연동 API")
public interface PaymentControllerDocs {

  @Operation(summary = "결제 요청", description = """
      결제하기 버튼 클릭 시 가장 먼저 호출하는 API입니다.
      V2 기준 주문 생성의 기본 엔드포인트이며, 토스 위젯 초기화에 필요한 값을 반환합니다.

      **처리 내용:**
      1. (V2) Redis 선점 유효성 검증 (만료/충돌/권한 확인)
      2. Reservation, Payment 생성 (PENDING 상태)
      3. 토스 위젯에 전달할 orderId/successUrl/failUrl 반환

      **요청 필드 핵심:**
      - scheduleId: 공연 회차 ID
      - scheduleSeatIds: 선택한 회차 좌석 ID 목록
      - bookerName/bookerPhone/bookerEmail: 예매자 정보

      **권장 호출 순서:**
      1. `/api/v2/reservations/reserve`로 좌석 선점
      2. `/api/v1/payment/request`로 orderId 발급
      3. 프론트에서 토스 SDK 호출

      **권한:** 인증된 사용자만 가능
      """)
  @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 생성 성공", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
          {
            "success": true,
            "data": {
              "orderId": "MOA-abc123def456ghi789jk",
              "amount": 136000,
              "orderName": "뮤지컬 <ANNE> 10th Anniversary - 2좌석",
              "booker": {
                "name": "홍길동",
                "email": "hong@example.com",
                "phone": "010-1234-5678"
              },
              "successUrl": "https://api.example.com/api/v1/payment/success?orderId=MOA-abc123def456ghi789jk",
              "failUrl": "https://api.example.com/api/v1/payment/fail?orderId=MOA-abc123def456ghi789jk"
            }
          }
          """))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증 실패(선점 만료/좌석 불일치 등)"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
  })
  ResponseEntity<ApiResponse<PaymentDto.RequestResponse>> requestPayment(
      @RequestBody(description = "결제 요청 정보", required = true) PaymentDto.Request request);

  @Operation(summary = "결제 승인", description = """
      결제 승인 API (POST)입니다.
      토스 SDK의 successUrl 리다이렉트는 GET 방식이므로
      기본적으로 `/api/v1/payment/success` 또는 `/api/v1/payment/confirm`(GET 호환)에서 처리합니다.
      프론트에서 수동 승인 플로우를 사용할 때만 이 API를 호출하세요.

      **3단 검증:**
      1. 좌석이 LOCKED 상태이고 본인이 선점했는지
      2. 선점 시간(5분)이 만료되지 않았는지
      3. 결제 금액이 일치하는지

      **검증 실패 시:** 토스 결제 취소 API 호출 후 예외 발생
      **검증 성공 시:** 토스 승인 API 호출, 상태 확정

      **권한:** 인증된 사용자(본인 예매만)
      """)
  ResponseEntity<ApiResponse<PaymentDto.SuccessResponse>> confirmPayment(
      @RequestBody(description = "결제 승인 요청 정보", required = true) PaymentDto.ConfirmRequest request);

  @Operation(summary = "결제 실패 처리", description = """
      토스 결제 인증 실패 또는 취소 시(failUrl 리다이렉트) 호출합니다.

      **처리 내용:**
      1. Payment → FAILED 상태로 변경, 실패 사유 저장
      2. Reservation → CANCELLED 상태로 변경
      3. 선점한 좌석(ScheduleSeat) 락 해제 → AVAILABLE

      **참고:** orderId가 없을 수 있음 (PAY_PROCESS_CANCELED 케이스)
      """)
  ResponseEntity<ApiResponse<Void>> failPayment(
      @RequestBody(description = "결제 실패 정보", required = true) PaymentDto.FailRequest request);

  @Operation(summary = "결제 성공 핸들러 (토스 리다이렉트)", description = """
      토스가 successUrl로 GET 리다이렉트할 때 호출됩니다.
      orderId, paymentKey, amount로 결제 승인 후 프론트 완료 페이지로 302 리다이렉트합니다.
      인증(쿠키) 필요. 미인증 시 완료 페이지 대신 실패 페이지로 리다이렉트합니다.
      백엔드만 테스트: noRedirect=1 및 app.payment.test-no-redirect=true 이면
      Mock 승인 후 200 + CompletionResponse JSON 반환 (302 없음).
      """)
  ResponseEntity<?> successHandler(
      @Parameter(description = "주문 번호") String orderId,
      @Parameter(description = "결제 키") String paymentKey,
      @Parameter(description = "결제 금액") Long amount,
      @Parameter(description = "리다이렉트 방지 여부 (1: 방지)") String noRedirect);

  @Operation(summary = "결제 실패 핸들러 (토스 리다이렉트)", description = """
      토스가 failUrl로 GET 리다이렉트할 때 호출됩니다.
      orderId, code, message로 실패 처리 후 프론트 실패 페이지로 302 리다이렉트합니다.
      orderId는 없을 수 있습니다 (PAY_PROCESS_CANCELED 등).
      백엔드만 테스트: noRedirect=1 및 app.payment.test-no-redirect=true 이면
      302 대신 200 + TestNoRedirectFailResponse JSON 반환.
      """)
  ResponseEntity<?> failHandler(
      @Parameter(description = "주문 번호") String orderId,
      @Parameter(description = "에러 코드") String code,
      @Parameter(description = "에러 메시지") String message,
      @Parameter(description = "리다이렉트 방지 여부 (1: 방지)") String noRedirect);

  @Operation(summary = "결제 완료 정보 조회", description = """
      예매 완료 페이지에서 공연/좌석/예매자/결제 정보를 조회합니다.

      `reservationNumber` 또는 `reservationId` 중 **하나만** 입력하세요.

      **실제 토스 결제 흐름:**
      `/payment/success` 핸들러 → 프론트 `/payment/complete?reservationNumber=RES-xxx` 리다이렉트
      → 프론트가 이 API 호출

      **Mock 결제 흐름:**
      `/payment/mock` 응답의 `reservationId`로 직접 호출

      **권한:** 인증된 사용자 (본인 예매만)
      """, responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
          {
            "success": true,
            "data": {
              "reservationId": 8,
              "performance": {
                "title": "K6 공석선점 테스트 공연",
                "date": "2026-03-10T19:30:00",
                "session": 1
              },
              "seats": [
                { "section": "VIP", "seatNumber": "A-1" }
              ],
              "booker": {
                "name": "홍길동",
                "phone": "010-1234-5678",
                "email": "hong@example.com"
              },
              "payment": {
                "method": "ETC",
                "amount": 150000,
                "paidAt": "2026-02-25T15:26:54"
              }
            }
          }
          """))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "파라미터 오류 (reservationNumber, reservationId 둘 다 입력하거나 둘 다 미입력)", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
          {
            "success": false,
            "data": null,
            "message": "reservationNumber 또는 reservationId 중 하나만 필수입니다."
          }
          """))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "예매 정보 없음 또는 본인 예매 아님")
  })
  @GetMapping("/complete")
  ResponseEntity<ApiResponse<PaymentDto.CompletionResponse>> getComplete(
      @Parameter(description = "예매 번호 (reservationNumber). reservationId와 둘 중 하나만 입력", example = "RES-20260225-9827CD") @RequestParam(required = false) String reservationNumber,
      @Parameter(description = "예약 ID. reservationNumber와 둘 중 하나만 입력", example = "8") @RequestParam(required = false) Long reservationId);
}
