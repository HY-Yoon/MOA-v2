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

        // 기존 파라미터에 prompt=consent 추가
        // consent: 항상 동의 화면 표시 (이미 동의한 경우에도 다시 표시하여 완전히 처음처럼 보이게 함)
        Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());
        
        // 제공자별 파라미터 설정
        String registrationId = authorizationRequest.getAttribute("registration_id");
        if (registrationId != null) {
            if ("google".equals(registrationId)) {
                // 구글: prompt=consent 사용
                additionalParameters.put("prompt", "consent");
            } else if ("naver".equals(registrationId)) {
                // 네이버: auth_type=reauthenticate 사용 (개인정보 동의 화면 강제 표시)
                // 네이버는 authorization-uri에 이미 auth_type=reauthenticate가 포함되어 있지만
                // 추가 파라미터로 강제할 수도 있음
                additionalParameters.put("auth_type", "reauthenticate");
            }
        } else {
            // 기본값: consent 사용
            additionalParameters.put("prompt", "consent");
        }

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();
    }
}

