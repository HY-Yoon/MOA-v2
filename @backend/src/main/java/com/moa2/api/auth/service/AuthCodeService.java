package com.moa2.api.auth.service;

import com.moa2.api.auth.domain.entity.AuthCode;
import com.moa2.api.auth.domain.repository.AuthCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * 소셜 로그인 일회용 인증 코드 관리 서비스
 * - OAuth2 로그인 성공 후 쿠키 대신 단기 코드를 발급
 * - 프론트엔드가 이 코드를 exchange-code API로 제시하면, 진짜 JWT 토큰으로 교환해줌
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCodeService {

    private static final long AUTH_CODE_TTL_SECONDS = 180L; // 3분

    private final AuthCodeRepository authCodeRepository;

    /**
     * 일회용 인증 코드(Auth Code) 생성 및 Redis 저장
     * @param email 사용자 이메일
     * @param provider 소셜 제공자 이름 (예: KAKAO, GOOGLE)
     * @return 생성된 코드 문자열
     */
    public String createAuthCode(String email, String provider) {
        String code = UUID.randomUUID().toString().replace("-", "");
        AuthCode authCode = new AuthCode(code, email, provider, AUTH_CODE_TTL_SECONDS);
        authCodeRepository.save(authCode);
        log.info("Auth Code 생성 완료 (TTL={}초): email={}", AUTH_CODE_TTL_SECONDS, maskEmail(email));
        return code;
    }

    /**
     * 코드로 Auth Code 조회
     * @param code 일회용 인증 코드
     * @return AuthCode (없으면 empty)
     */
    public Optional<AuthCode> findByCode(String code) {
        return authCodeRepository.findById(code);
    }

    /**
     * 코드 사용 후 즉시 삭제 (일회용 처리)
     * @param code 일회용 인증 코드
     */
    public void deleteCode(String code) {
        authCodeRepository.deleteById(code);
        log.info("Auth Code 삭제 완료 (일회용 처리)");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        return (local.length() > 2 ? local.substring(0, 2) : local) + "***@" + parts[1];
    }
}
