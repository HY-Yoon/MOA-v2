package com.moa2.global.handler;

import com.moa2.domain.user.entity.User;
import com.moa2.domain.user.repository.UserRepository;
import com.moa2.global.dto.OAuthAttributes;
import com.moa2.global.security.JwtTokenProvider;
import com.moa2.global.service.RefreshTokenService;
import com.moa2.global.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2 로그인 성공 시 Access Token과 Refresh Token을 생성하고 세션에 저장한 후 리다이렉트하는 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = null;
        
        // 1. OAuth2User attributes에서 직접 이메일 추출 시도 (구글 등)
        email = oAuth2User.getAttribute("email");
        
        // 2. 이메일이 없으면 DB에서 사용자 조회 (네이버 등 response 안에 있는 경우)
        if (email == null) {
            try {
                OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
                
                // OAuthAttributes로 변환하여 providerId 추출
                Map<String, Object> attributes = oAuth2User.getAttributes();
                OAuthAttributes oauthAttributes = OAuthAttributes.of(registrationId, attributes);
                
                // DB에서 사용자 조회
                User user = userRepository
                        .findBySocialProviderAndProviderId(oauthAttributes.getProvider(), oauthAttributes.getProviderId())
                        .orElse(null);
                
                if (user != null) {
                    email = user.getEmail();
                    log.debug("DB에서 사용자 이메일 조회 성공: {} ({})", 
                            LogMaskingUtil.maskEmail(email), oauthAttributes.getProvider());
                }
            } catch (Exception e) {
                log.warn("DB에서 사용자 조회 실패: {}", e.getMessage());
            }
        }

        if (email == null || email.trim().isEmpty()) {
            log.error("OAuth2 사용자 정보에서 이메일을 찾을 수 없습니다.");
            response.sendRedirect("/api/auth/error?message=이메일 정보를 찾을 수 없습니다.");
            return;
        }

        // Access Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(email);
        log.info("Access Token 생성 완료: {}", LogMaskingUtil.maskEmail(email));

        // Refresh Token 생성
        String refreshToken = jwtTokenProvider.createRefreshToken(email);
        log.info("Refresh Token 생성 완료: {}", LogMaskingUtil.maskEmail(email));

        // Refresh Token을 DB에 저장
        refreshTokenService.createRefreshToken(email, refreshToken);

        // DB에서 사용자 정보 조회 (socialProvider 확인용)
        User user = userRepository.findByEmail(email).orElse(null);
        String socialProviderName = "Google"; // 기본값
        if (user != null) {
            switch (user.getSocialProvider()) {
                case GOOGLE -> socialProviderName = "Google";
                case NAVER -> socialProviderName = "Naver";
                case KAKAO -> socialProviderName = "Kakao";
            }
        }

        // 세션에 토큰 및 제공자 정보 저장
        HttpSession session = request.getSession();
        session.setAttribute("access_token", accessToken);
        session.setAttribute("refresh_token", refreshToken);
        session.setAttribute("user_email", email);
        session.setAttribute("social_provider", socialProviderName);

        // 성공 페이지로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, "/api/auth/success");
    }
}

