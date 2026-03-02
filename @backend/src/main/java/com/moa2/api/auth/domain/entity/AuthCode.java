package com.moa2.api.auth.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

/**
 * 소셜 로그인 성공 후 발급되는 일회용 인증 코드 (Redis 저장)
 * - OAuth2 로그인 성공 시 쿠키 대신 이 코드를 생성하여 프론트엔드로 전달
 * - 프론트엔드는 이 코드로 /api/auth/exchange-code API를 호출하여 실제 JWT 토큰으로 교환
 * - TTL이 지나면 Redis에서 자동 삭제 (일회용)
 */
@Getter
@AllArgsConstructor
@RedisHash("auth_code")
public class AuthCode {

    /** 일회용 인증 코드 (UUID 기반 랜덤 문자열) */
    @Id
    private String code;

    /** 해당 코드와 매핑된 사용자 이메일 */
    @Indexed
    private String email;

    /** 해당 코드와 매핑된 소셜 제공자 이름 (예: KAKAO, GOOGLE) */
    private String provider;

    /** TTL (초): 기본 3분 후 자동 만료 */
    @TimeToLive
    private long ttl;
}
