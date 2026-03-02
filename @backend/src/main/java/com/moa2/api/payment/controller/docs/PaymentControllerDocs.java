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

  @Operation(summary = "예매자 확인 정보 조회", description = """
      결제 과정 중 예매자 확인 단계에서 호출합니다.

      **반환 정보:**
      - 이름 (name): 로그인 정보에 등록된 이름
      - 이메일 (email): 로그인 정보에 등록된 이메일
      - 연락처 (phone): 로그인 정보에 등록된 연락처 (없을 수 있음)

      **권한:** 인증된 사용자만 가능 (쿠키 기반 인증)
      """)
  ResponseEntity<ApiResponse<PaymentDto.BuyerInfoResponse>> getBuyerInfo();

  @Operation(summary = "결제 요청", description = """
      프론트에서 토스 결제 위젯을 띄우기 전에 호출합니다.

      **처리 내용:**
      1. 좌석 선점 상태 검증 (LOCKED 상태, 본인 선점 여부)
      2. Reservation, Payment 생성 (PENDING 상태)
      3. 토스 위젯에 전달할 정보 반환

      **권한:** 인증된 사용자만 가능
      """)
  ResponseEntity<ApiResponse<PaymentDto.RequestResponse>> requestPayment(
      @RequestBody(description = "결제 요청 정보", required = true) PaymentDto.Request request);

  @Operation(summary = "결제 승인", description = """
      토스 결제 인증 성공 후(successUrl 리다이렉트) 호출합니다.

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
