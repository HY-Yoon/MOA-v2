package com.moa2.api.payment.client;

/**
 * 토스 결제 취소 API 요청 DTO
 */
public record TossCancelRequest(
    String cancelReason
) {}
