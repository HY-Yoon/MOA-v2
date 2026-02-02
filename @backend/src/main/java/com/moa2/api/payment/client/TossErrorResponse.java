package com.moa2.api.payment.client;

/**
 * 토스 API 에러 응답 DTO
 */
public record TossErrorResponse(
    String code,
    String message
) {}
