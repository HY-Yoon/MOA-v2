package com.moa2.api.auth.service;

import com.moa2.api.auth.domain.entity.RefreshToken;
import com.moa2.api.auth.domain.repository.RefreshTokenRepository;
import com.moa2.api.auth.dto.AuthDto;
import com.moa2.global.exception.RefreshTokenException;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.JwtTokenProvider;
import com.moa2.global.util.LogMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Refresh Token 관리 서비스 (Redis 기반)
 *
 * [변경 이유]
 * 기존 JPA(DB) → Redis 마이그레이션
 * - TTL 자동 관리 (만료 시 Redis 자동 삭제, DB 스케줄러 불필요)
 * - 고성능 토큰 검증 (DB 조회 부하 없음)
 * - 토큰 무효화(로그아웃) 기능 유지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Refresh Token을 Redis에 저장
     * 동일한 email+provider 조합의 기존 토큰이 있으면 삭제 후 새로 저장 (One Token Per User Per Provider)
     */
    public RefreshToken createRefreshToken(String email, String refreshToken, SocialProvider socialProvider) {
        String providerName = socialProvider.name();

        // 기존 토큰 삭제 (동일 email + provider)
        List<RefreshToken> existing = refreshTokenRepository.findByUserEmailAndSocialProvider(email, providerName);
        if (!existing.isEmpty()) {
            existing.forEach(t -> refreshTokenRepository.deleteById(t.getToken()));
            log.debug("기존 Refresh Token 삭제: {} ({})", LogMaskingUtil.maskEmail(email), providerName);
        }

        // TTL = JWT Refresh Token 만료 시간(ms) → 초 단위 변환
        long ttlSeconds = jwtTokenProvider.getRefreshTokenExpiration() / 1000;

        RefreshToken token = new RefreshToken(refreshToken, email, providerName, ttlSeconds);
        RefreshToken saved = refreshTokenRepository.save(token);
        log.info("Refresh Token Redis 저장 완료: {} ({})", LogMaskingUtil.maskEmail(email), providerName);
        return saved;
    }

    /**
     * 토큰 문자열로 Refresh Token 조회
     */
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findById(token)
                .orElseThrow(() -> new RefreshTokenException.RefreshTokenNotFoundException(
                        "Refresh Token을 찾을 수 없습니다. 다시 로그인해주세요."
                ));
    }

    /**
     * 사용자 이메일로 Refresh Token 삭제 (로그아웃 시)
     */
    public void deleteByUserEmail(String email) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserEmail(email);
        tokens.forEach(t -> refreshTokenRepository.deleteById(t.getToken()));
        log.info("Refresh Token 삭제 완료: {}", LogMaskingUtil.maskEmail(email));
    }

    /**
     * 사용자 이메일 + 소셜 제공자로 Refresh Token 삭제 (로그아웃 시)
     */
    public void deleteByUserEmailAndSocialProvider(String email, SocialProvider socialProvider) {
        String providerName = socialProvider.name();
        List<RefreshToken> tokens = refreshTokenRepository.findByUserEmailAndSocialProvider(email, providerName);
        tokens.forEach(t -> refreshTokenRepository.deleteById(t.getToken()));
        log.info("Refresh Token 삭제 완료: {} ({})", LogMaskingUtil.maskEmail(email), providerName);
    }

    /**
     * 토큰 문자열로 삭제
     */
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteById(token);
        log.info("Refresh Token 삭제 완료 (by token)");
    }

    /**
     * Refresh Token으로 새로운 Access Token 발급
     * Redis에 저장된 토큰인지 검증 후 새 Access Token 발급
     */
    public AuthDto.TokenResponse refreshAccessToken(String refreshToken) {
        // 1. JWT 서명 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token 서명 또는 형식");
            throw new RefreshTokenException.InvalidGrantException(
                    "유효하지 않은 Refresh Token입니다. 다시 로그인해주세요."
            );
        }

        // 2. Redis에서 토큰 존재 여부 검증 (없으면 로그아웃된 것으로 간주)
        findByToken(refreshToken); // 없으면 내부에서 예외 throw

        // 3. 이메일 및 제공자 추출
        String email;
        String provider;
        try {
            email = jwtTokenProvider.getEmailFromRefreshToken(refreshToken);
            provider = jwtTokenProvider.getProviderFromRefreshToken(refreshToken);
            if (provider == null || provider.isEmpty()) {
                throw new RefreshTokenException.InvalidGrantException(
                        "토큰에 제공자 정보가 없습니다. 다시 로그인해주세요."
                );
            }
        } catch (RefreshTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Refresh Token 파싱 오류: {}", e.getMessage());
            throw new RefreshTokenException.InvalidGrantException(
                    "토큰 파싱 중 오류가 발생했습니다. 다시 로그인해주세요."
            );
        }

        // 4. 새 Access Token 발급 (role 포함)
        String role = jwtTokenProvider.getRoleFromRefreshToken(refreshToken);
        String newAccessToken = jwtTokenProvider.createAccessToken(email, provider, role);
        log.info("Access Token 갱신 완료: {} (role={})", LogMaskingUtil.maskEmail(email), role);

        return AuthDto.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .refreshTokenExpiresIn(jwtTokenProvider.getRefreshTokenExpiration())
                .email(email)
                .role(role)
                .build();
    }
}
