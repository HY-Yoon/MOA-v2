package com.moa2.api.payment.exception;

import lombok.Getter;

/**
 * 토스페이먼츠 API 호출 시 발생하는 예외
 */
@Getter
public class TossPaymentException extends RuntimeException {

    private final String code;

    public TossPaymentException(String code, String message) {
        super(message);
        this.code = code;
    }
}
