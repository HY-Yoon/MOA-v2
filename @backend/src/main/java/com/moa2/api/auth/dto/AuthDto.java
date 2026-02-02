package com.moa2.api.auth.dto;

import com.moa2.api.user.domain.entity.User;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * 인증 관련 DTO 통합 클래스
 */
public class AuthDto {

    /**
     * 사용자 정보 응답 DTO
     */
    @Schema(description = "사용자 정보 응답")
    @Builder
    public record UserInfoResponse(
            @Schema(description = "이메일", example = "test@example.com") String email,

            @Schema(description = "이름", example = "홍길동") String name,

            @Schema(description = "프로필 사진 URL", example = "https://example.com/profile.jpg") String picture,

            @Schema(description = "소셜 제공자", example = "GOOGLE") SocialProvider provider,

            @Schema(description = "제공자 ID", example = "1234567890") String providerId,

            @Schema(description = "사용자 권한", example = "USER") UserRole role) {
        public static UserInfoResponse from(User user) {
            return UserInfoResponse.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .picture(user.getPicture())
                    .provider(user.getSocialProvider())
                    .providerId(user.getProviderId())
                    .role(user.getRole())
                    .build();
        }
    }

    /**
     * JWT 토큰 검증 응답 DTO
     */
    @Schema(description = "토큰 검증 응답")
    @Builder
    public record TokenVerifyResponse(
            @Schema(description = "유효 여부", example = "true") boolean valid,

            @Schema(description = "메시지", example = "유효한 토큰입니다.") String message,

            @Schema(description = "사용자 정보 (유효한 경우)") UserInfoResponse user) {
        public static TokenVerifyResponse success(UserInfoResponse userInfo) {
            return TokenVerifyResponse.builder()
                    .valid(true)
                    .message("유효한 토큰입니다.")
                    .user(userInfo)
                    .build();
        }

        public static TokenVerifyResponse fail(String message) {
            return TokenVerifyResponse.builder()
                    .valid(false)
                    .message(message)
                    .user(null)
                    .build();
        }
    }

    /**
     * 토큰 응답 DTO - AccessToken과 RefreshToken 함께 반환
     */
    @Schema(description = "토큰 갱신 응답")
    @Builder
    public record TokenResponse(
            @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiIsIn...") String accessToken,

            @Schema(description = "Refresh Token", example = "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...") String refreshToken,

            @Schema(description = "Access Token 만료 시간 (밀리초)", example = "3600000") Long accessTokenExpiresIn,

            @Schema(description = "Refresh Token 만료 시간 (밀리초)", example = "1209600000") Long refreshTokenExpiresIn,

            @Schema(description = "사용자 이메일", example = "test@example.com") String email) {
    }

    /**
     * Refresh Token 요청 DTO
     */
    @Schema(description = "Refresh Token 요청")
    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh Token은 필수입니다.") @Schema(description = "Refresh Token", required = true) String refreshToken) {
    }
}
