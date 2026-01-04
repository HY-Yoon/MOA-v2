package com.moa2.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성, 검증, 파싱을 담당하는 컴포넌트
 * Access Token과 Refresh Token을 별도의 시크릿 키로 관리
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey accessTokenSecretKey;
    private final SecretKey refreshTokenSecretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.access.secret}") String accessSecret,
            @Value("${jwt.access.expiration:86400000}") long accessExpiration,
            @Value("${jwt.refresh.secret}") String refreshSecret,
            @Value("${jwt.refresh.expiration:604800000}") long refreshExpiration) {
        // Access Token용 SecretKey 생성
        this.accessTokenSecretKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        // Refresh Token용 SecretKey 생성 (별도의 시크릿 키 사용)
        this.refreshTokenSecretKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessExpiration;
        this.refreshTokenExpiration = refreshExpiration;
    }

    /**
     * 이메일을 기반으로 Access Token 생성
     * @param email 사용자 이메일
     * @return Access Token 문자열
     */
    public String createAccessToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(accessTokenSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 이메일을 기반으로 Refresh Token 생성
     * @param email 사용자 이메일
     * @return Refresh Token 문자열
     */
    public String createRefreshToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(refreshTokenSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * @deprecated createAccessToken을 사용하세요
     * 이메일을 기반으로 JWT 토큰 생성 (하위 호환성 유지)
     */
    @Deprecated
    public String createToken(String email) {
        return createAccessToken(email);
    }

    /**
     * Access Token 검증
     * @param token Access Token 문자열
     * @return 검증 성공 여부
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token, accessTokenSecretKey);
    }

    /**
     * Refresh Token 검증
     * @param token Refresh Token 문자열
     * @return 검증 성공 여부
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshTokenSecretKey);
    }

    /**
     * JWT 토큰 검증 (내부 메서드)
     * @param token JWT 토큰 문자열
     * @param secretKey 검증에 사용할 SecretKey
     * @return 검증 성공 여부
     */
    private boolean validateToken(String token, SecretKey secretKey) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT 토큰이 만료되었습니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("지원하지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 비어있습니다: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
        }
        return false;
    }

    /**
     * @deprecated validateAccessToken을 사용하세요
     * JWT 토큰 검증 (하위 호환성 유지)
     */
    @Deprecated
    public boolean validateToken(String token) {
        return validateAccessToken(token);
    }

    /**
     * Access Token에서 이메일 추출
     * @param token Access Token 문자열
     * @return 사용자 이메일
     */
    public String getEmailFromAccessToken(String token) {
        return getEmailFromToken(token, accessTokenSecretKey);
    }

    /**
     * Refresh Token에서 이메일 추출
     * @param token Refresh Token 문자열
     * @return 사용자 이메일
     */
    public String getEmailFromRefreshToken(String token) {
        return getEmailFromToken(token, refreshTokenSecretKey);
    }

    /**
     * JWT 토큰에서 이메일 추출 (내부 메서드)
     * @param token JWT 토큰 문자열
     * @param secretKey 파싱에 사용할 SecretKey
     * @return 사용자 이메일
     */
    private String getEmailFromToken(String token, SecretKey secretKey) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * @deprecated getEmailFromAccessToken을 사용하세요
     * JWT 토큰에서 이메일 추출 (하위 호환성 유지)
     */
    @Deprecated
    public String getEmailFromToken(String token) {
        return getEmailFromAccessToken(token);
    }

    /**
     * Access Token 만료 시간 반환 (밀리초)
     * @return 만료 시간 (밀리초)
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Refresh Token 만료 시간 반환 (밀리초)
     * @return 만료 시간 (밀리초)
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}

