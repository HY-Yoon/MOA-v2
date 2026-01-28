package com.moa2.api.payment.client;

/**
 * 토스 결제 승인 API 요청 DTO
 */
public record TossConfirmRequest(
    String paymentKey,
    String orderId,
    Long amount
) {}
