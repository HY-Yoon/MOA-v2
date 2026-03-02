package com.moa2.api.auth.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

/**
 * Refresh Token Redis 엔티티
 *
 * [변경 이유]
 * 기존 JPA(DB) 방식에서 Redis 방식으로 마이그레이션.
 * - TTL 자동 관리 (만료 시 Redis가 알아서 삭제)
 * - 고성능 I/O (JWT 검증 시 DB 부하 없음)
 * - 토큰 무효화(로그아웃) 지원 유지
 *
 * [저장 키 구조]
 * refresh_token:{token값}  →  email + provider 정보 저장
 * 인덱스: email + provider 조합으로 조회/삭제 가능
 */
@Getter
@AllArgsConstructor
@RedisHash("refresh_token")
public class RefreshToken {

    /** Refresh Token JWT 문자열 (PK) */
    @Id
    private String token;

    /** 사용자 이메일 */
    @Indexed
    private String userEmail;

    /** 소셜 제공자 이름 (예: KAKAO, GOOGLE) */
    @Indexed
    private String socialProvider;

    /** TTL (초): 기본 14일 */
    @TimeToLive
    private long ttl;
}
