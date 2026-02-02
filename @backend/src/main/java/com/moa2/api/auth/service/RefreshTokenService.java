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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Refresh Token 관리 서비스
 * DB에 저장된 Refresh Token을 관리하고 Access Token 갱신 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Refresh Token을 DB에 저장
     * 기존 Refresh Token이 있으면 삭제하고 새로 저장 (One Token Per User Per Provider)
     * @param email 사용자 이메일
     * @param refreshToken Refresh Token 문자열
     * @param socialProvider 소셜 제공자
     * @return 저장된 RefreshToken 엔티티
     */
    @Transactional
    public RefreshToken createRefreshToken(String email, String refreshToken, SocialProvider socialProvider) {
        // 기존 Refresh Token이 있으면 삭제 (같은 이메일 + 같은 제공자)
        if (refreshTokenRepository.existsByUserEmailAndSocialProvider(email, socialProvider)) {
            refreshTokenRepository.deleteByUserEmailAndSocialProvider(email, socialProvider);
            log.debug("기존 Refresh Token 삭제: {} ({})", LogMaskingUtil.maskEmail(email), socialProvider);
        }

        // 만료 시간 계산 (7일)
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000);

        // 새 Refresh Token 저장
        RefreshToken token = RefreshToken.builder()
                .token(refreshToken)
                .userEmail(email)
                .socialProvider(socialProvider)
                .expiryDate(expiryDate)
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(token);
        log.info("Refresh Token 저장 완료: {} ({})", LogMaskingUtil.maskEmail(email), socialProvider);
        return savedToken;
    }

    /**
     * Refresh Token 만료 확인 및 예외 처리
     * @param token RefreshToken 엔티티
     * @throws RefreshTokenException.RefreshTokenExpiredException 만료된 경우
     */
    public void verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenException.RefreshTokenExpiredException(
                    "Refresh Token이 만료되었습니다. 다시 로그인해주세요."
            );
        }
    }

    /**
     * 토큰 문자열로 Refresh Token 조회
     * @param token Refresh Token 문자열
     * @return RefreshToken 엔티티
     * @throws RefreshTokenException.RefreshTokenNotFoundException 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RefreshTokenException.RefreshTokenNotFoundException(
                        "Refresh Token을 찾을 수 없습니다."
                ));
    }

    /**
     * 사용자 이메일로 Refresh Token 삭제 (로그아웃 시)
     * @param email 사용자 이메일
     */
    @Transactional
    public void deleteByUserEmail(String email) {
        refreshTokenRepository.deleteByUserEmail(email);
        log.info("Refresh Token 삭제 완료: {}", LogMaskingUtil.maskEmail(email));
    }

    /**
     * 사용자 이메일과 소셜 제공자로 Refresh Token 삭제 (로그아웃 시)
     * @param email 사용자 이메일
     * @param socialProvider 소셜 제공자
     */
    @Transactional
    public void deleteByUserEmailAndSocialProvider(String email, SocialProvider socialProvider) {
        refreshTokenRepository.deleteByUserEmailAndSocialProvider(email, socialProvider);
        log.info("Refresh Token 삭제 완료: {} ({})", LogMaskingUtil.maskEmail(email), socialProvider);
    }

    /**
     * 토큰으로 Refresh Token 삭제
     * @param token Refresh Token 문자열
     */
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
        log.info("Refresh Token 삭제 완료");
    }

    /**
     * Refresh Token으로 새로운 Access Token 발급
     * @param refreshToken Refresh Token 문자열
     * @return TokenResponse (새 Access Token + 기존 Refresh Token 정보)
     */
    @Transactional
    public AuthDto.TokenResponse refreshAccessToken(String refreshToken) {
        // 1. Refresh Token 검증 (JWT 서명 검증)
        // JWT 형식이 맞지 않거나 서명이 유효하지 않은 경우
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token 서명 또는 형식: {}", LogMaskingUtil.maskToken(refreshToken));
            throw new RefreshTokenException.InvalidGrantException(
                    "유효하지 않은 Refresh Token 형식입니다. 다시 로그인해주세요."
            );
        }

        // 2. DB에서 Refresh Token 조회 (존재하지 않는 경우)
        RefreshToken token;
        try {
            token = findByToken(refreshToken);
        } catch (RefreshTokenException.RefreshTokenNotFoundException e) {
            // DB에 존재하지 않는 토큰
            log.warn("DB에서 Refresh Token을 찾을 수 없음");
            throw e; // 원본 예외 그대로 전파
        }

        // 3. 만료 확인
        try {
            verifyExpiration(token);
        } catch (RefreshTokenException.RefreshTokenExpiredException e) {
            // 만료된 토큰
            log.warn("Refresh Token이 만료됨: {}", LogMaskingUtil.maskEmail(token.getUserEmail()));
            throw e; // 원본 예외 그대로 전파
        }

        // 4. 이메일 및 제공자 추출
        String email;
        String provider;
        try {
            email = jwtTokenProvider.getEmailFromRefreshToken(refreshToken);
            provider = jwtTokenProvider.getProviderFromRefreshToken(refreshToken);
            
            // provider가 없으면 예외 발생 (구형 토큰일 수 있음)
            if (provider == null || provider.isEmpty()) {
                log.warn("Refresh Token에 provider 정보가 없습니다. 구형 토큰일 수 있습니다: {}", LogMaskingUtil.maskEmail(email));
                throw new RefreshTokenException.InvalidGrantException(
                        "토큰에 제공자 정보가 없습니다. 다시 로그인해주세요."
                );
            }
        } catch (RefreshTokenException e) {
            throw e; // 이미 처리된 예외는 그대로 전파
        } catch (Exception e) {
            // JWT 파싱 오류
            log.error("Refresh Token에서 정보 추출 실패: {}", e.getMessage());
            throw new RefreshTokenException.InvalidGrantException(
                    "토큰 파싱 중 오류가 발생했습니다. 다시 로그인해주세요."
            );
        }

        // 5. 새 Access Token 생성 (provider 정보 포함)
        String newAccessToken = jwtTokenProvider.createAccessToken(email, provider);

        log.info("Access Token 갱신 완료: {}", LogMaskingUtil.maskEmail(email));

        // 6. TokenResponse 반환
        return AuthDto.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // 기존 Refresh Token 유지
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .refreshTokenExpiresIn(jwtTokenProvider.getRefreshTokenExpiration())
                .email(email)
                .build();
    }
}

