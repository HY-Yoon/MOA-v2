package com.moa2.api.payment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 결제 관련 비즈니스 예외
 */
@Getter
public class PaymentException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public PaymentException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    // 자주 사용되는 예외 팩토리 메서드들
    public static PaymentException notFound(String message) {
        return new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", message);
    }

    public static PaymentException invalidState(String message) {
        return new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_STATE", message);
    }

    public static PaymentException seatNotLocked(String message) {
        return new PaymentException(HttpStatus.BAD_REQUEST, "SEAT_NOT_LOCKED", message);
    }

    public static PaymentException lockExpired(String message) {
        return new PaymentException(HttpStatus.BAD_REQUEST, "LOCK_EXPIRED", message);
    }

    public static PaymentException amountMismatch(String message) {
        return new PaymentException(HttpStatus.BAD_REQUEST, "AMOUNT_MISMATCH", message);
    }

    public static PaymentException unauthorized(String message) {
        return new PaymentException(HttpStatus.FORBIDDEN, "UNAUTHORIZED", message);
    }
}
