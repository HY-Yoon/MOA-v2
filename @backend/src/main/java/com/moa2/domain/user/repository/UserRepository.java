package com.moa2.domain.user.repository;

import com.moa2.domain.user.entity.User;
import com.moa2.global.model.SocialProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 이름 또는 이메일로 검색 (LIKE 검색)
     * phone은 검색 대상에서 제외
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "u.name LIKE %:keyword% OR u.email LIKE %:keyword%)")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 소셜 제공자와 제공자 ID로 사용자 조회 (OAuth2 로그인용)
     */
    Optional<User> findBySocialProviderAndProviderId(SocialProvider socialProvider, String providerId);

    /**
     * 이메일로 사용자 조회 (JWT 토큰 검증용)
     */
    Optional<User> findByEmail(String email);
}

