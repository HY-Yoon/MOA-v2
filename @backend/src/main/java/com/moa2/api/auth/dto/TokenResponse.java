package com.moa2.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 응답 DTO
 * Access Token과 Refresh Token을 함께 반환
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    /**
     * Access Token (API 인증에 사용)
     */
    private String accessToken;

    /**
     * Refresh Token (Access Token 갱신에 사용)
     */
    private String refreshToken;

    /**
     * Access Token 만료 시간 (밀리초)
     */
    private Long accessTokenExpiresIn;

    /**
     * Refresh Token 만료 시간 (밀리초)
     */
    private Long refreshTokenExpiresIn;

    /**
     * 사용자 이메일
     */
    private String email;
}

