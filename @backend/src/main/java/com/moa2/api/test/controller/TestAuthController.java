package com.moa2.api.test.controller;

import com.moa2.api.test.dto.TestLoginRequest;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.JwtTokenProvider;
import com.moa2.api.auth.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * local/test 프로필에서만 활성화되는 테스트용 인증 API.
 * - k6 같은 도구가 OAuth2 브라우저 로그인을 수행하기 어려운 점을 보완
 * - accessToken/refreshToken을 Set-Cookie로 내려줌
 */
@Slf4j
@RestController
@RequestMapping("/api/test/auth")
@RequiredArgsConstructor
public class TestAuthController {

        private final JwtTokenProvider jwtTokenProvider;
        private final RefreshTokenService refreshTokenService;
        private final UserRepository userRepository;

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<Map<String, String>>> login(
                        @Valid @RequestBody TestLoginRequest request,
                        HttpServletResponse response) {
                String email = request.getEmail();
                String providerStr = request.getProvider();

                SocialProvider socialProvider;
                try {
                        socialProvider = SocialProvider.valueOf(providerStr);
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error("유효하지 않은 provider 입니다: " + providerStr));
                }

                // 테스트용 사용자 upsert (email + provider 유니크)
                User user = userRepository.findByEmailAndSocialProvider(email, socialProvider)
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .email(email)
                                                .socialProvider(socialProvider)
                                                .providerId("test-" + email)
                                                .name("test-user")
                                                .build()));

                // 토큰 생성 (provider, role 포함)
                String provider = user.getSocialProvider().name();
                String role = user.getRole().name();
                String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), provider, role);
                String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), provider, role);

                // refresh token 저장 (refresh API/로그아웃 흐름과 동일하게 유지)
                refreshTokenService.createRefreshToken(user.getEmail(), refreshToken, user.getSocialProvider());

                long accessMaxAgeSeconds = Math.max(1, jwtTokenProvider.getAccessTokenExpiration() / 1000);
                long refreshMaxAgeSeconds = Math.max(1, jwtTokenProvider.getRefreshTokenExpiration() / 1000);

                // NOTE:
                // - 로컬/테스트 환경에서 k6(http)로 호출하는 경우가 많아 secure=false로 설정
                // - 브라우저 Cross-Site 쿠키 전략(SameSite=None + Secure=true)은 OAuth2SuccessHandler가
                // 담당
                ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                                .path("/")
                                .httpOnly(true)
                                .secure(false)
                                .sameSite("Lax")
                                .maxAge(accessMaxAgeSeconds)
                                .build();

                ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                                .path("/")
                                .httpOnly(true)
                                .secure(false)
                                .sameSite("Lax")
                                .maxAge(refreshMaxAgeSeconds)
                                .build();

                response.addHeader("Set-Cookie", accessTokenCookie.toString());
                response.addHeader("Set-Cookie", refreshTokenCookie.toString());

                log.info("Test login issued cookies: email={}, provider={}", email, provider);
                return ResponseEntity.ok(ApiResponse.success(Map.of(
                                "email", user.getEmail(),
                                "provider", provider)));
        }
}
