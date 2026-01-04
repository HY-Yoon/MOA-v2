package com.moa2.global.exception;

/**
 * Refresh Token 관련 예외
 */
public class RefreshTokenException extends RuntimeException {

    public RefreshTokenException(String message) {
        super(message);
    }

    public RefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Refresh Token을 찾을 수 없을 때 발생하는 예외
     */
    public static class RefreshTokenNotFoundException extends RefreshTokenException {
        public RefreshTokenNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Refresh Token이 만료되었을 때 발생하는 예외
     */
    public static class RefreshTokenExpiredException extends RefreshTokenException {
        public RefreshTokenExpiredException(String message) {
            super(message);
        }
    }

    /**
     * Invalid grant 에러 (Refresh Token이 무효화된 경우)
     */
    public static class InvalidGrantException extends RefreshTokenException {
        public InvalidGrantException(String message) {
            super(message);
        }
    }
}

