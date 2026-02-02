package com.moa2.api.payment.controller;

import com.moa2.api.payment.dto.MockPaymentRequest;
import com.moa2.api.payment.dto.PaymentDto;
import com.moa2.api.payment.exception.PaymentException;
import com.moa2.api.payment.service.PaymentService;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock 결제 API (K6 부하 테스트 및 프론트엔드 연동 테스트용)
 * - 외부 토스 API 호출 없이 DB 상태 변경만 수행
 */
@Slf4j
@Tag(name = "Mock 결제 API", description = "K6 부하 테스트 / 프론트 연동 테스트용 Mock 결제 API")
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class MockPaymentController {

        private final com.moa2.api.payment.facade.PaymentFacade paymentFacade;

        /**
         * Mock 결제 승인
         * PaymentService.confirmPayment(paymentKey, orderId, amount)를 호출하여
         * 토스 API 없이 DB만 갱신 후 PaymentSuccessResponse 반환
         */
        @Operation(summary = "Mock 결제 승인", description = "토스 API 호출 없이 DB 상태 변경만 수행하여 K6 테스트를 지원합니다.")
        @PostMapping("/mock")
        public ResponseEntity<ApiResponse<PaymentDto.PaymentSuccessResponse>> mockConfirm(
                        @Valid @RequestBody MockPaymentRequest request) {
                try {
                        PaymentDto.PaymentSuccessResponse response = paymentFacade.confirmPaymentMock(
                                        request.paymentKey(),
                                        request.orderId(),
                                        request.amount());
                        return ResponseEntity.ok(ApiResponse.success(response));
                } catch (Exception e) {
                        log.warn("Mock 결제 승인 실패: orderId={}, message={}", request.orderId(), e.getMessage());

                        // PaymentException인 경우 해당 status 사용, 그 외에는 400 Bad Request
                        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.BAD_REQUEST;
                        if (e instanceof PaymentException pe) {
                                status = pe.getStatus();
                        }

                        // code를 null로 전달하여 응답 JSON에서 제외되도록 함
                        return ResponseEntity.status(status)
                                        .body(ApiResponse.error(e.getMessage()));
                }
        }
}
