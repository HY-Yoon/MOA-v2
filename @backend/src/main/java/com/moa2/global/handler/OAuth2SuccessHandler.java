package com.moa2.global.handler;

import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.api.auth.dto.OAuthAttributes;
import com.moa2.global.security.JwtTokenProvider;
import com.moa2.api.auth.service.RefreshTokenService;
import com.moa2.global.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2 로그인 성공 시 Access Token과 Refresh Token을 생성하고 쿠키에 저장한 후 리다이렉트하는 핸들러
 * Cross-Site 환경(백엔드: Koyeb, 프론트엔드: Local/Vercel)을 지원하기 위해 SameSite=None 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    // Cross-Site 쿠키 설정 (환경변수로 제어 가능)
    @Value("${security.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${security.cookie.same-site:None}")
    private String cookieSameSite;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        User user = null;
        
        try {
            // providerId로 사용자 조회 (가장 정확한 방법)
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
            
            // OAuthAttributes로 변환하여 providerId 추출
            Map<String, Object> attributes = oAuth2User.getAttributes();
            OAuthAttributes oauthAttributes = OAuthAttributes.of(registrationId, attributes);
            
            // DB에서 사용자 조회 (providerId로 조회 - 가장 정확함)
            user = userRepository
                    .findBySocialProviderAndProviderId(oauthAttributes.getProvider(), oauthAttributes.getProviderId())
                    .orElse(null);
            
            if (user == null) {
                log.error("사용자 정보를 찾을 수 없습니다: {} ({})", 
                        oauthAttributes.getProviderId(), oauthAttributes.getProvider());
                response.sendRedirect("/api/auth/error?message=사용자 정보를 찾을 수 없습니다.");
                return;
            }
            
            log.debug("사용자 조회 성공: {} ({})", 
                    LogMaskingUtil.maskEmail(user.getEmail()), user.getSocialProvider());
            
        } catch (Exception e) {
            log.error("사용자 조회 실패: {}", e.getMessage(), e);
            response.sendRedirect("/api/auth/error?message=사용자 정보 조회 중 오류가 발생했습니다.");
            return;
        }

        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            log.error("사용자 이메일이 없습니다: {}", user.getId());
            response.sendRedirect("/api/auth/error?message=이메일 정보를 찾을 수 없습니다.");
            return;
        }

        // Access Token 생성 (provider 정보 포함)
        String provider = user.getSocialProvider().name();
        String accessToken = jwtTokenProvider.createAccessToken(email, provider);
        log.info("Access Token 생성 완료: {} ({})", LogMaskingUtil.maskEmail(email), user.getSocialProvider());

        // Refresh Token 생성 (provider 정보 포함)
        String refreshToken = jwtTokenProvider.createRefreshToken(email, provider);
        log.info("Refresh Token 생성 완료: {} ({})", LogMaskingUtil.maskEmail(email), user.getSocialProvider());

        // Refresh Token을 DB에 저장 (소셜 제공자 포함)
        refreshTokenService.createRefreshToken(email, refreshToken, user.getSocialProvider());

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                .path("/")                          // 모든 경로에서 쿠키 사용 가능
                .httpOnly(true)                     // JavaScript 접근 차단 (XSS 방지)
                .secure(cookieSecure)               // HTTPS에서만 전송 (SameSite=None 사용 시 필수)
                .sameSite(cookieSameSite)           // Cross-Site 요청 허용 (None으로 설정)
                .maxAge(30 * 60)                    // 30분 (초 단위)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .path("/")                          // 모든 경로에서 쿠키 사용 가능
                .httpOnly(true)                     // JavaScript 접근 차단 (XSS 방지)
                .secure(cookieSecure)               // HTTPS에서만 전송 (SameSite=None 사용 시 필수)
                .sameSite(cookieSameSite)           // Cross-Site 요청 허용 (None으로 설정)
                .maxAge(14 * 24 * 60 * 60)          // 14일 (초 단위)
                .build();
        
        // Set-Cookie 헤더에 쿠키 추가
        response.addHeader("Set-Cookie", accessTokenCookie.toString());
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());
        
        log.info("OAuth2 로그인 성공: {} ({}) - Cross-Site Cookie 설정 완료 (SameSite={}, Secure={})", 
                LogMaskingUtil.maskEmail(email), user.getSocialProvider(), cookieSameSite, cookieSecure);

        // 성공 페이지로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, "/api/auth/success");
    }
}

