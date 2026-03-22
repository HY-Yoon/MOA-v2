package com.moa2.api.payment.facade;

import com.moa2.api.payment.client.TossPaymentClient;
import com.moa2.api.payment.client.TossPaymentResponse;
import com.moa2.api.payment.dto.PaymentDto;
import com.moa2.api.payment.exception.PaymentException;
import com.moa2.api.payment.exception.TossPaymentException;
import com.moa2.api.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;
    private final TossPaymentClient tossPaymentClient;

    /**
     * Facade 패턴: 결제 승인
     * 1. [DB Transaction] 결제 정보 검증 및 선점 (Locking & State Update to IN_PROGRESS)
     * 2. [External API] 토스 결제 승인 (No DB Transaction) -> DB 락 없이 수행
     * 3. [DB Transaction] 결제 완료 처리 (State Update to COMPLETED)
     * 4. [Kafka] 결제 완료 알림 이벤트 발행 (V2 프로필에서만)
     */
    public PaymentDto.SuccessResponse confirmPayment(PaymentDto.ConfirmRequest request, Long userId) {
        // Step 1. 결제 검증 및 진행 상태로 변경 (트랜잭션 A)
        paymentService.preparePayment(request.orderId(), request.amount().longValue(), userId);

        TossPaymentResponse tossResponse;
        try {
            // Step 2. 토스 결제 승인 요청 (트랜잭션 없음, 락 없음)
            log.info("토스 결제 승인 요청 시작: orderId={}", request.orderId());
            tossResponse = tossPaymentClient.confirmPayment(
                    request.paymentKey(), request.orderId(), request.amount());
        } catch (TossPaymentException e) {
            log.error("토스 결제 승인 실패: {}", e.getMessage());
            paymentService.failPaymentProcessing(request.orderId(), e.getMessage());
            throw PaymentException.invalidState("결제 승인에 실패했습니다: " + e.getMessage());
        }

        // Step 3. 결제 완료 처리 (트랜잭션 B)
        try {
            PaymentDto.SuccessResponse response = paymentService.completePayment(
                    request.orderId(), request.paymentKey(), tossResponse);

            return response;
        } catch (Exception e) {
            // [CRITICAL] 결제 승인은 성공했으나, DB 반영 실패 시 -> 결제 취소(환불) 처리
            log.error("결제 완료 처리 중 오류 발생 (DB Commit Fail). 자동 취소(환불)를 진행합니다. orderId={}, reason={}",
                    request.orderId(), e.getMessage());

            try {
                tossPaymentClient.cancelPayment(tossResponse.paymentKey(), "System Error: Final DB Commit Failed");
            } catch (Exception cancelEx) {
                log.error("자동 취소 실패! 수동 확인이 필요합니다. paymentKey={}, error={}", tossResponse.paymentKey(),
                        cancelEx.getMessage());
            }

            try {
                paymentService.failPaymentProcessing(request.orderId(), "DB Commit Failed during completion");
            } catch (Exception failEx) {
                log.error("DB FAILED 상태 변경 실패. orderId={}", request.orderId());
            }

            throw PaymentException.invalidState("결제 마무리 중 오류가 발생하여 자동 취소되었습니다.");
        }
    }

    /**
     * Facade 패턴: Mock 결제 승인
     */
    public PaymentDto.PaymentSuccessResponse confirmPaymentMock(String paymentKey, String orderId, Long amount) {
        // Step 1. 결제 검증 및 진행 상태로 변경 (트랜잭션 A)
        paymentService.preparePaymentMock(orderId, amount);

        // Step 2. 외부 API 호출 시뮬레이션
        log.info("Mock 외부 결제 시스템 승인 처리 중... (Simulation)");

        // Step 3. 결제 완료 처리 (트랜잭션 B)
        try {
            PaymentDto.PaymentSuccessResponse response = paymentService.completePaymentMock(paymentKey, orderId, amount);

            return response;
        } catch (Exception e) {
            log.error("Mock 결제 완료 처리 중 오류 발생: {}", e.getMessage());
            paymentService.failPaymentProcessing(orderId, "Mock System Error: " + e.getMessage());
            throw e;
        }
    }

}
