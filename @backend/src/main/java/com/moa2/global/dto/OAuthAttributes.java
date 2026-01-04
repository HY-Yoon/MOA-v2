package com.moa2.global.dto;

import com.moa2.domain.user.entity.User;
import com.moa2.global.model.SocialProvider;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * OAuth2 제공자로부터 받은 사용자 정보를 담는 DTO
 */
@Getter
@Builder
public class OAuthAttributes {
    private String name;
    private String email;
    private String picture;
    private String phone;
    private String providerId;
    private SocialProvider provider;

    /**
     * OAuth2 제공자별 응답을 OAuthAttributes로 변환
     * @param providerName 제공자 이름 (google, kakao, naver 등)
     * @param attributes OAuth2 제공자로부터 받은 사용자 정보
     * @return OAuthAttributes 객체
     */
    public static OAuthAttributes of(String providerName, Map<String, Object> attributes) {
        if ("google".equals(providerName)) {
            return ofGoogle(attributes);
        } else if ("naver".equals(providerName)) {
            return ofNaver(attributes);
        }
        throw new IllegalArgumentException("지원하지 않는 제공자입니다: " + providerName);
    }

    /**
     * Google OAuth2 응답 파싱
     * @param attributes Google OAuth2 응답
     * @return OAuthAttributes 객체
     */
    private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .providerId((String) attributes.get("sub"))
                .provider(SocialProvider.GOOGLE)
                .build();
    }

    /**
     * Naver OAuth2 응답 파싱
     * @param attributes Naver OAuth2 응답 (response 키 안에 실제 데이터)
     * @return OAuthAttributes 객체
     */
    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
        // 네이버는 response 키 안에 실제 사용자 정보가 있음
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            throw new IllegalArgumentException("네이버 OAuth2 응답에 response 키가 없습니다.");
        }

        // id는 필수값
        String id = (String) response.get("id");
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("네이버 OAuth2 응답에 id가 없습니다.");
        }

        // email, name, profile_image, mobile은 선택적 (사용자가 제공 거부 가능)
        String email = (String) response.get("email");
        String name = (String) response.get("name");
        String profileImage = (String) response.get("profile_image");
        String mobile = (String) response.get("mobile"); // 네이버는 mobile 필드로 전화번호 제공

        // email이 null이면 기본값 설정
        if (email == null || email.trim().isEmpty()) {
            email = "no-email-" + id + "@naver.local";
        }

        // name이 null이면 기본값 설정
        if (name == null || name.trim().isEmpty()) {
            name = "네이버 사용자";
        }

        return OAuthAttributes.builder()
                .name(name)
                .email(email)
                .picture(profileImage)
                .phone(mobile) // 전화번호 추가
                .providerId(id)
                .provider(SocialProvider.NAVER)
                .build();
    }

    /**
     * OAuthAttributes를 User 엔티티로 변환
     * @return User 엔티티
     */
    public User toEntity() {
        return User.builder()
                .email(email)
                .name(name)
                .picture(picture)
                .phone(phone)
                .providerId(providerId)
                .socialProvider(provider)
                .build();
    }
}

