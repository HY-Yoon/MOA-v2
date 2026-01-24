package com.moa2.api.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

/**
 * 토스 결제 승인/취소 API 응답 DTO (Payment 객체)
 * 필요한 필드만 정의
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentResponse(
    String mId,
    String version,
    String paymentKey,
    String orderId,
    String orderName,
    String status,           // DONE, CANCELED 등
    OffsetDateTime requestedAt,
    OffsetDateTime approvedAt,
    String method,           // 카드, 가상계좌, 계좌이체 등
    Integer totalAmount,
    Integer balanceAmount,
    String currency,
    CardInfo card,
    EasyPayInfo easyPay,
    FailureInfo failure
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CardInfo(
        String issuerCode,
        String acquirerCode,
        String number,
        Integer installmentPlanMonths,
        Boolean isInterestFree,
        String cardType,
        String ownerType,
        String approveNo,
        Integer amount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EasyPayInfo(
        String provider,
        Integer amount,
        Integer discountAmount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FailureInfo(
        String code,
        String message
    ) {}
}
