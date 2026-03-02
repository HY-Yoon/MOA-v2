package com.moa2.global.config;

import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * OAuth2 인증 요청에 추가 파라미터를 설정하는 리졸버
 * - 기존 회원(쿠키에 refreshToken 있음): 동의 화면 안 뜸
 * - 신규 회원(쿠키에 refreshToken 없음): 동의 화면 뜸
 */
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request) {
        if (authorizationRequest == null) {
            return null;
        }

        // 프론트엔드 환경 파라미터 저장 (local or prod)
        String env = request.getParameter("env");
        if (env != null) {
            HttpSession session = request.getSession();
            session.setAttribute("oauth2_env", env);
        }

        // Map<String, Object> additionalParameters = new
        // HashMap<>(authorizationRequest.getAdditionalParameters());
        //
        // // 제공자별 파라미터 설정
        // String registrationId = authorizationRequest.getAttribute("registration_id");
        // if (registrationId != null) {
        // if ("google".equals(registrationId)) {
        // additionalParameters.put("prompt", "consent");
        // } else if ("naver".equals(registrationId)) {
        // additionalParameters.put("auth_type", "reprompt");
        // }
        // } else {
        // additionalParameters.put("prompt", "consent");
        // }

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                // .additionalParameters(additionalParameters)
                .redirectUri(authorizationRequest.getRedirectUri())
                .build();
    }

}
