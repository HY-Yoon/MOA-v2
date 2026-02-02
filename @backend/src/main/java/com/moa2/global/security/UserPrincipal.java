package com.moa2.global.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * JWT 기반 인증에서 principal로 사용하는 사용자 식별 정보.
 * - email: 사용자 이메일
 * - provider: 소셜 로그인 제공자 (KAKAO/NAVER/GOOGLE 등)
 */
@Getter
@RequiredArgsConstructor
public class UserPrincipal {
    private final String email;
    private final String provider;
}
