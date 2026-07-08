package com.moa2.api.payment.facade;

import com.moa2.api.payment.client.TossPaymentClient;
import com.moa2.api.payment.client.TossPaymentResponse;
import com.moa2.api.payment.dto.PaymentDto;
import com.moa2.api.payment.exception.PaymentException;
import com.moa2.api.payment.exception.TossPaymentException;
import com.moa2.api.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 결제 승인 중 외부 PG 성공/실패와 내부 상태 전이가 엇갈려 결제 유실·중복 승인으로 이어지는 사고를 막는다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @InjectMocks
    private PaymentFacade paymentFacade;

    @Test
    @DisplayName("정상 흐름은 결제를 진행중으로 준비한 뒤 토스 승인 성공 후 완료 처리한다")
    void 정상_흐름은_결제를_진행중으로_준비한_뒤_토스_승인_성공_후_완료_처리한다() {
        // given
        PaymentDto.ConfirmRequest request = new PaymentDto.ConfirmRequest("payment-key", "order-1", 50_000L);
        TossPaymentResponse tossResponse = tossResponse("payment-key", "order-1");
        PaymentDto.SuccessResponse successResponse = successResponse("payment-key", "order-1");

        when(tossPaymentClient.confirmPayment("payment-key", "order-1", 50_000L)).thenReturn(tossResponse);
        when(paymentService.completePayment("order-1", "payment-key", tossResponse)).thenReturn(successResponse);

        // when
        paymentFacade.confirmPayment(request, 1L);

        // then
        InOrder inOrder = inOrder(paymentService, tossPaymentClient);
        inOrder.verify(paymentService).preparePayment("order-1", 50_000L, 1L);
        inOrder.verify(tossPaymentClient).confirmPayment("payment-key", "order-1", 50_000L);
        inOrder.verify(paymentService).completePayment("order-1", "payment-key", tossResponse);
    }

    @Test
    @DisplayName("토스 승인 실패 시 결제 실패 처리 후 예외를 던진다")
    void 토스_승인_실패_시_결제_실패_처리_후_예외를_던진다() {
        // given
        PaymentDto.ConfirmRequest request = new PaymentDto.ConfirmRequest("payment-key", "order-1", 50_000L);
        TossPaymentException tossException = new TossPaymentException("REJECTED", "토스 승인 거절");

        when(tossPaymentClient.confirmPayment("payment-key", "order-1", 50_000L)).thenThrow(tossException);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirmPayment(request, 1L))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("토스 승인 거절");

        InOrder inOrder = inOrder(paymentService, tossPaymentClient);
        inOrder.verify(paymentService).preparePayment("order-1", 50_000L, 1L);
        inOrder.verify(tossPaymentClient).confirmPayment("payment-key", "order-1", 50_000L);
        inOrder.verify(paymentService).failPaymentProcessing("order-1", "토스 승인 거절");
        verifyNoInteractionsAfterTossFailure();
    }

    @Test
    @DisplayName("토스 승인 후 완료 처리에서 예외가 나면 토스 취소와 실패 처리를 시도한다")
    void 토스_승인_후_완료_처리에서_예외가_나면_토스_취소와_실패_처리를_시도한다() {
        // given
        PaymentDto.ConfirmRequest request = new PaymentDto.ConfirmRequest("payment-key", "order-1", 50_000L);
        TossPaymentResponse tossResponse = tossResponse("payment-key", "order-1");

        when(tossPaymentClient.confirmPayment("payment-key", "order-1", 50_000L)).thenReturn(tossResponse);
        when(paymentService.completePayment("order-1", "payment-key", tossResponse))
                .thenThrow(new RuntimeException("DB commit failed"));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirmPayment(request, 1L))
                .isInstanceOf(PaymentException.class);

        InOrder inOrder = inOrder(paymentService, tossPaymentClient);
        inOrder.verify(paymentService).preparePayment("order-1", 50_000L, 1L);
        inOrder.verify(tossPaymentClient).confirmPayment("payment-key", "order-1", 50_000L);
        inOrder.verify(paymentService).completePayment("order-1", "payment-key", tossResponse);
        inOrder.verify(tossPaymentClient).cancelPayment("payment-key", "System Error: Final DB Commit Failed");
        inOrder.verify(paymentService).failPaymentProcessing("order-1", "DB Commit Failed during completion");
        // 리스크: 토스 취소와 failPaymentProcessing이 모두 실패하면 현재 코드는 예외만 반환하고,
        // DB 결제가 IN_PROGRESS로 남거나 외부 결제 승인과 내부 실패 상태가 불일치할 수 있다.
    }

    private void verifyNoInteractionsAfterTossFailure() {
        verify(paymentService).failPaymentProcessing("order-1", "토스 승인 거절");
    }

    private TossPaymentResponse tossResponse(String paymentKey, String orderId) {
        return new TossPaymentResponse(
                "mid",
                "2022-11-16",
                paymentKey,
                orderId,
                "MOA ticket",
                "DONE",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "카드",
                50_000,
                50_000,
                "KRW",
                null,
                null,
                null
        );
    }

    private PaymentDto.SuccessResponse successResponse(String paymentKey, String orderId) {
        return new PaymentDto.SuccessResponse(
                10L,
                "R-1",
                orderId,
                paymentKey,
                50_000,
                "카드",
                "MOA ticket",
                LocalDateTime.now()
        );
    }
}
