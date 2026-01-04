package com.moa2.global.handler;

import com.moa2.global.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 로그인 실패 시 처리하는 핸들러
 * Google OAuth 인증 실패, 네트워크 오류, 사용자 취소 등을 처리
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       AuthenticationException exception) throws IOException {
        
        String errorMessage = "로그인에 실패했습니다. 다시 시도해주세요.";
        String errorCode = "unknown_error";
        int statusCode = HttpServletResponse.SC_BAD_REQUEST;

        // OAuth2 인증 예외 처리
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2Error error = ((OAuth2AuthenticationException) exception).getError();
            errorCode = error.getErrorCode();
            String errorDescription = error.getDescription();
            
            // 에러 코드별 사용자 친화적 메시지
            errorMessage = getErrorMessage(errorCode, errorDescription);
            statusCode = getHttpStatusCode(errorCode);
            
            // 로그에 상세 정보 기록 (민감 정보 마스킹)
            log.error("OAuth2 인증 실패 - ErrorCode: {}, Description: {}, URI: {}", 
                    errorCode, 
                    LogMaskingUtil.mask(errorDescription), 
                    error.getUri());
        } else {
            // 일반 인증 예외
            log.error("인증 실패: {}", exception.getMessage());
            if (exception.getCause() != null) {
                log.error("원인: {}", exception.getCause().getMessage());
            }
        }

        // 에러 페이지로 리다이렉트
        String redirectUrl = "/api/auth/error?message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8) 
                + "&code=" + URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
        
        log.info("OAuth2 로그인 실패 - 사용자를 에러 페이지로 리다이렉트: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * 에러 코드별 사용자 친화적 메시지 반환
     */
    private String getErrorMessage(String errorCode, String errorDescription) {
        return switch (errorCode) {
            case "invalid_grant" -> "인증이 만료되었거나 유효하지 않습니다. 다시 로그인해주세요.";
            case "invalid_client" -> "인증 서버 설정 오류가 발생했습니다. 관리자에게 문의해주세요.";
            case "invalid_request" -> "잘못된 인증 요청입니다. 다시 시도해주세요.";
            case "unauthorized_client" -> "인증되지 않은 클라이언트입니다. 관리자에게 문의해주세요.";
            case "access_denied" -> "로그인이 취소되었습니다.";
            case "unsupported_response_type" -> "지원하지 않는 응답 형식입니다. 관리자에게 문의해주세요.";
            case "invalid_scope" -> "요청한 권한이 유효하지 않습니다. 관리자에게 문의해주세요.";
            case "server_error" -> "Google 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
            case "temporarily_unavailable" -> "Google 서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.";
            default -> {
                log.warn("알 수 없는 OAuth2 에러 코드: {}", errorCode);
                yield "로그인 중 오류가 발생했습니다. 다시 시도해주세요.";
            }
        };
    }

    /**
     * 에러 코드별 HTTP 상태 코드 반환
     */
    private int getHttpStatusCode(String errorCode) {
        return switch (errorCode) {
            case "invalid_grant", "unauthorized_client" -> HttpServletResponse.SC_UNAUTHORIZED;
            case "invalid_client", "invalid_request", "invalid_scope" -> HttpServletResponse.SC_BAD_REQUEST;
            case "server_error", "temporarily_unavailable" -> HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            default -> HttpServletResponse.SC_BAD_REQUEST;
        };
    }

}

