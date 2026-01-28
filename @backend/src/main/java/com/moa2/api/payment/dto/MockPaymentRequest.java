package com.moa2.api.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Mock 결제 API 요청 DTO (K6 부하 테스트 / 프론트 연동 테스트용)
 */
public record MockPaymentRequest(
        @NotBlank(message = "paymentKey는 필수입니다")
        String paymentKey,

        @NotBlank(message = "orderId는 필수입니다")
        String orderId,

        @NotNull(message = "amount는 필수입니다")
        Long amount
) {}
