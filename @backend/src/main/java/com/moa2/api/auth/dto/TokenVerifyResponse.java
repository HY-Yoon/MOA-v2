package com.moa2.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JWT 토큰 검증 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVerifyResponse {
    private boolean valid;
    private String message;
    private UserInfoResponse user;
}

