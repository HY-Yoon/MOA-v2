package com.moa2.global.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 인증 요청에 추가 파라미터를 설정하는 리졸버
 * prompt=consent를 추가하여 항상 동의 화면이 표시되도록 함
 * (이미 동의한 경우에도 다시 동의 화면을 표시하여 신규 회원 테스트 가능)
 */
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomOAuth2AuthorizationRequestResolver(OAuth2AuthorizationRequestResolver defaultResolver) {
        this.defaultResolver = defaultResolver;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(authorizationRequest);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            return null;
        }

        Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());

        // 제공자별 파라미터 설정
        String registrationId = authorizationRequest.getAttribute("registration_id");
        if (registrationId != null) {
            if ("google".equals(registrationId)) {
                additionalParameters.put("prompt", "consent");
            } else if ("naver".equals(registrationId)) {
                additionalParameters.put("auth_type", "reauthenticate");
            }
        } else {
            additionalParameters.put("prompt", "consent");
        }

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .redirectUri(authorizationRequest.getRedirectUri())
                .build();
    }
}

