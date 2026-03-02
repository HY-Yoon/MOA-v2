package com.moa2.global.handler;

import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.api.auth.dto.OAuthAttributes;
import com.moa2.api.auth.service.AuthCodeService;
import com.moa2.global.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2 로그인 성공 시 일회용 인증 코드(Auth Code)를 발급하고 프론트엔드로 리다이렉트하는 핸들러
 *
 * [변경 이유]
 * 기존에는 로그인 성공 후 JWT 토큰을 Set-Cookie로 구워서 프론트엔드로 리다이렉트 했으나,
 * 프론트(Vercel)와 백(Koyeb)의 도메인이 달라 브라우저가 서드파티 쿠키를 차단함.
 *
 * [변경 내용]
 * 1. JWT 토큰을 직접 발급하지 않고, 3분짜리 일회용 코드(Auth Code)를 Redis에 저장
 * 2. 프론트엔드로 리다이렉트할 때 URL에 code만 담아서 전달 (?code=...)
 * 3. 프론트엔드는 이 코드로 /api/auth/exchange-code API를 호출하여 실제 토큰(JSON)을 교환
 * 4. 프론트엔드가 자기 도메인에서 직접 HttpOnly 쿠키를 설정하므로 도메인 문제 해결
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthCodeService authCodeService;
    private final UserRepository userRepository;

    @Value("${app.frontend.local-url:http://localhost:3000}")
    private String frontendLocalUrl;

    @Value("${app.frontend.prod-url:https://moa-v2.vercel.app}")
    private String frontendProdUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        User user = null;

        try {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            String registrationId = oauth2Token.getAuthorizedClientRegistrationId();

            Map<String, Object> attributes = oAuth2User.getAttributes();
            OAuthAttributes oauthAttributes = OAuthAttributes.of(registrationId, attributes);

            user = userRepository
                    .findBySocialProviderAndProviderId(oauthAttributes.getProvider(),
                            oauthAttributes.getProviderId())
                    .orElse(null);

            if (user == null) {
                log.error("사용자 정보를 찾을 수 없습니다: {} ({})",
                        oauthAttributes.getProviderId(), oauthAttributes.getProvider());
                response.sendRedirect(resolveTargetUrl(request) + "/login?error=user_not_found");
                return;
            }

            log.debug("사용자 조회 성공: {} ({})",
                    LogMaskingUtil.maskEmail(user.getEmail()), user.getSocialProvider());

        } catch (Exception e) {
            log.error("사용자 조회 실패: {}", e.getMessage(), e);
            response.sendRedirect(resolveTargetUrl(request) + "/login?error=server_error");
            return;
        }

        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            log.error("사용자 이메일이 없습니다: {}", user.getId());
            response.sendRedirect(resolveTargetUrl(request) + "/login?error=no_email");
            return;
        }

        // 일회용 Auth Code 생성 및 Redis 저장 (JWT 토큰 직접 발급 X)
        String provider = user.getSocialProvider().name();
        String authCode = authCodeService.createAuthCode(email, provider);

        log.info("OAuth2 로그인 성공 - Auth Code 발급: {} ({})",
                LogMaskingUtil.maskEmail(email), user.getSocialProvider());

        // 프론트엔드 리다이렉트 URL 결정
        String targetBaseUrl = resolveTargetUrl(request);

        // 실제 JWT 토큰 대신 일회용 코드만 URL에 담아 리다이렉트
        String redirectUrl = targetBaseUrl + "/api/auth/callback?code=" + authCode;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * 환경(로컬/배포)에 따라 프론트엔드 기본 URL 반환
     */
    private String resolveTargetUrl(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String env = "prod"; // 기본값
        if (session != null) {
            String sessionEnv = (String) session.getAttribute("oauth2_env");
            if (sessionEnv != null) {
                env = sessionEnv;
                session.removeAttribute("oauth2_env");
            }
        }
        return "local".equalsIgnoreCase(env) ? frontendLocalUrl : frontendProdUrl;
    }
}
