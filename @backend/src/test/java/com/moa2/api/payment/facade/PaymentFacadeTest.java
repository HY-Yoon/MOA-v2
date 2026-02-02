package com.moa2.api.payment.facade;

import com.moa2.api.payment.client.TossPaymentClient;
import com.moa2.api.payment.client.TossPaymentResponse;
import com.moa2.api.payment.dto.PaymentDto;
import com.moa2.api.payment.exception.PaymentException;
import com.moa2.api.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    @InjectMocks
    private PaymentFacade paymentFacade;

    @Mock
    private PaymentService paymentService;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @Test
    @DisplayName("결제 승인은 성공했으나 DB 반영 실패 시 자동 취소(환불)가 호출되어야 한다")
    void confirmPayment_failSafe() {
        // given
        PaymentDto.ConfirmRequest request = new PaymentDto.ConfirmRequest("test_key", "order_123", 10000L);
        Long userId = 1L;

        // Step 1: Prepare success
        doNothing().when(paymentService).preparePayment(anyString(), anyLong(), anyLong());

        // Step 2: Toss Success
        TossPaymentResponse tossResponse = new TossPaymentResponse(
                "test_mid",                    // mId
                "2023-01-01",                  // version
                "test_key",                    // paymentKey
                "order_123",                   // orderId
                "뮤지컬",                      // orderName
                "DONE",                        // status
                OffsetDateTime.now(),          // requestedAt
                OffsetDateTime.now(),          // approvedAt
                "카드",                        // method
                10000,                         // totalAmount
                0,                             // balanceAmount
                "KRW",                         // currency
                null,                          // card
                null,                          // easyPay
                null                           // failure
        );
        given(tossPaymentClient.confirmPayment(anyString(), anyString(), anyLong()))
                .willReturn(tossResponse);

        // Step 3: DB Complete Fail (simulate DB error)
        given(paymentService.completePayment(anyString(), anyString(), any(TossPaymentResponse.class)))
                .willThrow(new RuntimeException("DB Connection Error"));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirmPayment(request, userId))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("자동 취소되었습니다");

        // verify cancel called
        verify(tossPaymentClient, times(1)).cancelPayment(eq("test_key"), anyString());

        // verify fail processing called
        verify(paymentService, times(1)).failPaymentProcessing(eq("order_123"), anyString());
    }
}
