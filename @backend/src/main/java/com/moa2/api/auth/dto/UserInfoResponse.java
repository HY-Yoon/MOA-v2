package com.moa2.api.auth.dto;

import com.moa2.domain.user.entity.User;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String email;
    private String name;
    private String picture;
    private SocialProvider provider;
    private UserRole role;

    /**
     * User 엔티티를 UserInfoResponse로 변환
     * @param user User 엔티티
     * @return UserInfoResponse 객체
     */
    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .picture(user.getPicture())
                .provider(user.getSocialProvider())
                .role(user.getRole())
                .build();
    }
}

