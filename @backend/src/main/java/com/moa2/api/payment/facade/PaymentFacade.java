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
     */
    public PaymentDto.SuccessResponse confirmPayment(PaymentDto.ConfirmRequest request, Long userId) {
        // Step 1. 결제 검증 및 진행 상태로 변경 (트랜잭션 A)
        // 이 단계가 끝나면 DB 락이 해제됨. 하지만 상태는 IN_PROGRESS이므로 다른 요청이 들어와도 막힘.
        paymentService.preparePayment(request.orderId(), request.amount().longValue(), userId);

        TossPaymentResponse tossResponse;
        try {
            // Step 2. 토스 결제 승인 요청 (트랜잭션 없음, 락 없음)
            log.info("토스 결제 승인 요청 시작: orderId={}", request.orderId());
            tossResponse = tossPaymentClient.confirmPayment(
                    request.paymentKey(), request.orderId(), request.amount());
        } catch (TossPaymentException e) {
            // 실패 시 결제 실패 처리 (DB 상태 원복 등)
            log.error("토스 결제 승인 실패: {}", e.getMessage());
            paymentService.failPaymentProcessing(request.orderId(), e.getMessage());
            throw PaymentException.invalidState("결제 승인에 실패했습니다: " + e.getMessage());
        }

        // Step 3. 결제 완료 처리 (트랜잭션 B)
        try {
            return paymentService.completePayment(request.orderId(), request.paymentKey(), tossResponse);
        } catch (Exception e) {
            // [CRITICAL] 3-1. 결제 승인은 성공했으나, DB 반영 실패 시 -> 결제 취소(환불) 처리 (보상 트랜잭션)
            log.error("결제 완료 처리 중 오류 발생 (DB Commit Fail). 자동 취소(환불)를 진행합니다. orderId={}, reason={}",
                    request.orderId(), e.getMessage());

            try {
                tossPaymentClient.cancelPayment(tossResponse.paymentKey(), "System Error: Final DB Commit Failed");
            } catch (Exception cancelEx) {
                log.error("자동 취소 실패! 수동 확인이 필요합니다. paymentKey={}, error={}", tossResponse.paymentKey(),
                        cancelEx.getMessage());
                // 여기서 Admin 알림 등을 발송해야 함.
            }

            // 실패 처리 (DB 상태 FAILED로 변경 시도)
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
     * 1. [DB Transaction] 결제 정보 검증 및 선점 (Locking & State Update to IN_PROGRESS)
     * 2. [Simulation] 외부 API 호출 시간 시뮬레이션 (No DB Transaction) -> DB 락 없이 수행
     * 3. [DB Transaction] 결제 완료 처리 (State Update to COMPLETED)
     */
    public PaymentDto.PaymentSuccessResponse confirmPaymentMock(String paymentKey, String orderId, Long amount) {
        // Step 1. 결제 검증 및 진행 상태로 변경 (트랜잭션 A)
        paymentService.preparePaymentMock(orderId, amount);

        // Step 2. 외부 API 호출 시뮬레이션 (약 500ms 지연 가정)
        // 실제 외부 연동이 없으므로 단순 로깅 처리하거나 Thread.sleep()을 줄 수 있음.
        log.info("Mock 외부 결제 시스템 승인 처리 중... (Simulation)");

        // Step 3. 결제 완료 처리 (트랜잭션 B)
        try {
            return paymentService.completePaymentMock(paymentKey, orderId, amount);
        } catch (Exception e) {
            log.error("Mock 결제 완료 처리 중 오류 발생: {}", e.getMessage());
            // 실패 시 상태 롤백 (FAILED)
            paymentService.failPaymentProcessing(orderId, "Mock System Error: " + e.getMessage());
            throw e;
        }
    }
}
