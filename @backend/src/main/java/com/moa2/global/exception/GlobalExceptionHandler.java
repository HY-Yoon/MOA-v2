package com.moa2.global.exception;

import com.moa2.global.dto.ApiResponse;
import com.moa2.global.util.LogMaskingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleRuntimeException(RuntimeException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        
        // 메시지에 따라 적절한 HTTP 상태 코드 설정
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (e.getMessage() != null && e.getMessage().contains("찾을 수 없습니다")) {
            status = HttpStatus.NOT_FOUND;
        }
        
        return ResponseEntity.status(status)
            .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        // 상세한 에러 메시지 생성
        String detailMessage = errors.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .reduce((a, b) -> a + ", " + b)
            .orElse("입력값 검증에 실패했습니다");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("입력값 검증에 실패했습니다: " + detailMessage));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleIllegalArgumentException(
            IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(ApiResponse.error("파일 크기가 너무 큽니다. 최대 크기를 초과했습니다."));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMultipartException(
            MultipartException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("파일 업로드 처리 중 오류가 발생했습니다: " + e.getMessage()));
    }

    @ExceptionHandler(RefreshTokenException.RefreshTokenNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleRefreshTokenNotFoundException(
            RefreshTokenException.RefreshTokenNotFoundException e) {
        log.warn("Refresh Token을 찾을 수 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("Refresh Token을 찾을 수 없습니다. 다시 로그인해주세요."));
    }

    @ExceptionHandler(RefreshTokenException.RefreshTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleRefreshTokenExpiredException(
            RefreshTokenException.RefreshTokenExpiredException e) {
        log.warn("Refresh Token 만료: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Refresh Token이 만료되었습니다. 다시 로그인해주세요."));
    }

    @ExceptionHandler(RefreshTokenException.InvalidGrantException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleInvalidGrantException(
            RefreshTokenException.InvalidGrantException e) {
        log.warn("Invalid grant 에러: {}", e.getMessage());
        // InvalidGrantException의 메시지를 그대로 사용 (이미 구체적인 메시지가 포함됨)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleRefreshTokenException(
            RefreshTokenException e) {
        log.error("Refresh Token 예외: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleOAuth2AuthenticationException(
            org.springframework.security.oauth2.core.OAuth2AuthenticationException e) {
        org.springframework.security.oauth2.core.OAuth2Error error = e.getError();
        String errorCode = error.getErrorCode();
        String errorMessage = getOAuth2ErrorMessage(errorCode);
        
        log.error("OAuth2 인증 예외: ErrorCode={}, Description={}", 
                errorCode, 
                LogMaskingUtil.mask(error.getDescription()));
        
        HttpStatus status = getOAuth2HttpStatus(errorCode);
        return ResponseEntity.status(status)
            .body(ApiResponse.error(errorMessage));
    }

    /**
     * OAuth2 에러 코드별 사용자 친화적 메시지
     */
    private String getOAuth2ErrorMessage(String errorCode) {
        return switch (errorCode) {
            case "invalid_grant" -> "인증이 만료되었습니다. 다시 로그인해주세요.";
            case "invalid_client" -> "인증 서버 설정 오류가 발생했습니다.";
            case "invalid_request" -> "잘못된 인증 요청입니다.";
            case "unauthorized_client" -> "인증되지 않은 클라이언트입니다.";
            case "access_denied" -> "로그인이 취소되었습니다.";
            case "server_error" -> "Google 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
            case "temporarily_unavailable" -> "Google 서비스가 일시적으로 사용할 수 없습니다.";
            default -> "OAuth2 인증 중 오류가 발생했습니다.";
        };
    }

    /**
     * OAuth2 에러 코드별 HTTP 상태 코드
     */
    private HttpStatus getOAuth2HttpStatus(String errorCode) {
        return switch (errorCode) {
            case "invalid_grant", "unauthorized_client" -> HttpStatus.UNAUTHORIZED;
            case "invalid_client", "invalid_request" -> HttpStatus.BAD_REQUEST;
            case "server_error", "temporarily_unavailable" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

}

