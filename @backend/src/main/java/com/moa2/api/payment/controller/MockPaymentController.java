package com.moa2.api.payment.controller;

import com.moa2.api.payment.dto.MockPaymentRequest;
import com.moa2.api.payment.dto.PaymentDto;
import com.moa2.api.payment.exception.PaymentException;
import com.moa2.api.payment.service.PaymentNotificationProducer;
import com.moa2.api.payment.service.PaymentService;
import com.moa2.api.reservation.domain.entity.Payment;
import com.moa2.api.reservation.domain.repository.PaymentRepository;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        private final PaymentService paymentService;
        private final PaymentRepository paymentRepository;
        private final PaymentNotificationProducer paymentNotificationProducer;

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

                        try {
                                String email = resolveUserInfo(request.orderId(), request.paymentKey()).email();
                                paymentNotificationProducer.sendPaymentNotification(request.orderId(), email);
                        } catch (Exception kafkaEx) {
                                log.warn("[Mock] 결제 알림 Kafka 이벤트 발행 실패: {}", kafkaEx.getMessage());
                        }

                        return ResponseEntity.ok(ApiResponse.success(response));
                } catch (Exception e) {
                        log.warn("Mock 결제 승인 실패: orderId={}, message={}", request.orderId(), e.getMessage());

                        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.BAD_REQUEST;
                        if (e instanceof PaymentException pe) {
                                status = pe.getStatus();
                        }

                        return ResponseEntity.status(status)
                                        .body(ApiResponse.error(e.getMessage()));
                }
        }

        /**
         * Mock 결제 승인 + 완료 정보 반환 (프론트 없이 백엔드 단독 테스트용)
         * - 토스 API 호출 없이 결제를 완료 처리한 뒤 완료 페이지 DTO를 즉시 내려준다.
         */
        @Operation(summary = "Mock 결제 완료", description = "토스 API 없이 결제를 완료 처리하고 완료 페이지 정보를 반환합니다.")
        @PostMapping("/mock/complete")
        public ResponseEntity<ApiResponse<PaymentDto.CompletionResponse>> mockComplete(
                        @Valid @RequestBody MockPaymentRequest request) {
                try {
                        paymentFacade.confirmPaymentMock(
                                        request.paymentKey(),
                                        request.orderId(),
                                        request.amount());

                        try {
                                String email = resolveUserInfo(request.orderId(), request.paymentKey()).email();
                                paymentNotificationProducer.sendPaymentNotification(request.orderId(), email);
                        } catch (Exception kafkaEx) {
                                log.warn("[Mock/complete] 결제 알림 Kafka 이벤트 발행 실패: {}", kafkaEx.getMessage());
                        }

                        Long userId = resolveUserInfo(request.orderId(), request.paymentKey()).userId();
                        PaymentDto.CompletionResponse completion = paymentService.getCompletionInfoByPaymentIdentifiers(
                                        request.orderId(), request.paymentKey(), userId);
                        return ResponseEntity.ok(ApiResponse.success(completion));
                } catch (PaymentException e) {
                        log.warn("Mock 결제 완료 실패: orderId={}, message={}", request.orderId(), e.getMessage());
                        return ResponseEntity.status(e.getStatus())
                                        .body(ApiResponse.error(e.getMessage(), e.getCode(), null));
                } catch (Exception e) {
                        log.warn("Mock 결제 완료 처리 중 예외: orderId={}, message={}", request.orderId(), e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(ApiResponse.error(e.getMessage()));
                }
        }

        /**
         * Mock 완료 정보 조회
         * - 이미 완료된 결제건을 orderId/paymentKey 기준으로 조회한다.
         */
        @Operation(summary = "Mock 결제 완료 정보 조회", description = "orderId 또는 paymentKey로 완료 정보를 조회합니다.")
        @GetMapping("/mock/complete")
        public ResponseEntity<ApiResponse<PaymentDto.CompletionResponse>> getMockComplete(
                        @RequestParam(required = false) String orderId,
                        @RequestParam(required = false) String paymentKey) {
                boolean hasOrderId = orderId != null && !orderId.isBlank();
                boolean hasPaymentKey = paymentKey != null && !paymentKey.isBlank();
                if (!hasOrderId && !hasPaymentKey) {
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error("orderId 또는 paymentKey 중 하나는 필수입니다."));
                }
                try {
                        Long userId = resolveUserInfo(orderId, paymentKey).userId();
                        PaymentDto.CompletionResponse response = paymentService.getCompletionInfoByPaymentIdentifiers(
                                        orderId, paymentKey, userId);
                        return ResponseEntity.ok(ApiResponse.success(response));
                } catch (PaymentException e) {
                        log.warn("Mock 완료 조회 실패: orderId={}, paymentKey={}, message={}", orderId, paymentKey, e.getMessage());
                        return ResponseEntity.status(e.getStatus())
                                        .body(ApiResponse.error(e.getMessage(), e.getCode(), null));
                }
        }

        private record UserInfo(Long userId, String email) {}

        private UserInfo resolveUserInfo(String orderId, String paymentKey) {
                Payment payment;
                if (orderId != null && !orderId.isBlank()) {
                        payment = paymentRepository.findByOrderIdWithReservationReadOnly(orderId)
                                        .orElseThrow(() -> PaymentException.notFound("결제 정보를 찾을 수 없습니다."));
                } else {
                        payment = paymentRepository.findByPaymentKeyWithReservationReadOnly(paymentKey)
                                        .orElseThrow(() -> PaymentException.notFound("결제 정보를 찾을 수 없습니다."));
                }
                var user = payment.getReservation().getUser();
                return new UserInfo(user.getId(), user.getEmail());
        }
}
