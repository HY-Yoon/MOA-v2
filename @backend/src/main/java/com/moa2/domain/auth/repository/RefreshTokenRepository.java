package com.moa2.domain.auth.repository;

import com.moa2.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Refresh Token Repository
 * 나중에 Redis로 마이그레이션 시 이 인터페이스만 수정하면 됨
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰으로 Refresh Token 조회
     * @param token Refresh Token 문자열
     * @return RefreshToken 엔티티
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자 이메일로 Refresh Token 조회
     * @param userEmail 사용자 이메일
     * @return RefreshToken 엔티티
     */
    Optional<RefreshToken> findByUserEmail(String userEmail);

    /**
     * 사용자 이메일로 Refresh Token 삭제 (로그아웃 시)
     * @param userEmail 사용자 이메일
     */
    void deleteByUserEmail(String userEmail);

    /**
     * 토큰으로 Refresh Token 삭제
     * @param token Refresh Token 문자열
     */
    void deleteByToken(String token);

    /**
     * 사용자 이메일로 Refresh Token 존재 여부 확인
     * @param userEmail 사용자 이메일
     * @return 존재 여부
     */
    boolean existsByUserEmail(String userEmail);
}

