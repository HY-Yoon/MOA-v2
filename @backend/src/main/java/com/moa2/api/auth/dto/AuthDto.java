package com.moa2.api.auth.dto;

import com.moa2.api.user.domain.entity.User;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    /**
     *  사용자 정보 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoResponse {
        private String email;
        private String name;
        private String picture;
        private SocialProvider provider;
        private String providerId;
        private UserRole role;

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
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenVerifyResponse {
        private boolean valid;
        private String message;
        private UserInfoResponse user;

        // 성공 응답 생성용 팩토리 메서드
        public static TokenVerifyResponse success(UserInfoResponse userInfo) {
            return TokenVerifyResponse.builder()
                    .valid(true)
                    .message("유효한 토큰입니다.")
                    .user(userInfo)
                    .build();
        }

        // 실패 응답 생성용 팩토리 메서드
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
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private Long accessTokenExpiresIn;
        private Long refreshTokenExpiresIn;
        private String email;
    }

    /**
     * Refresh Token 요청 DTO
     */
    public record RefreshTokenRequest (
        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
    ){}

}
