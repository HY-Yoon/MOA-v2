package com.moa2.api.auth.domain.repository;

import com.moa2.api.auth.domain.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

/**
 * Refresh Token Redis Repository
 * - Spring Data Redis CrudRepository 기반
 * - @Indexed 설정으로 email, provider 기반 조회/삭제 지원
 */
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    /** 토큰으로 조회 (PK 조회이므로 findById 사용 권장, 하위 호환용) */
    Optional<RefreshToken> findByToken(String token);

    /** 이메일로 조회 */
    List<RefreshToken> findByUserEmail(String userEmail);

    /** 이메일 + 소셜 제공자로 조회 */
    List<RefreshToken> findByUserEmailAndSocialProvider(String userEmail, String socialProvider);

    /** 이메일로 삭제 */
    void deleteByUserEmail(String userEmail);

    /** 이메일 + 소셜 제공자로 삭제 */
    void deleteByUserEmailAndSocialProvider(String userEmail, String socialProvider);

    /** 이메일로 존재 여부 확인 */
    boolean existsByUserEmail(String userEmail);

    /** 이메일 + 소셜 제공자로 존재 여부 확인 */
    boolean existsByUserEmailAndSocialProvider(String userEmail, String socialProvider);
}
