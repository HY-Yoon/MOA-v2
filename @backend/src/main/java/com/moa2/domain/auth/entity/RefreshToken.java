package com.moa2.domain.auth.entity;

import com.moa2.domain.auth.converter.RefreshTokenConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refresh Token 엔티티
 * DB에 저장하여 Access Token 갱신에 사용
 * 나중에 Redis로 마이그레이션 가능하도록 단순한 구조로 설계
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_user_email", columnList = "user_email"),
    @Index(name = "idx_refresh_token_token", columnList = "token")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    @Convert(converter = RefreshTokenConverter.class)
    private String token;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public RefreshToken(String token, String userEmail, LocalDateTime expiryDate) {
        this.token = token;
        this.userEmail = userEmail;
        this.expiryDate = expiryDate;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Refresh Token이 만료되었는지 확인
     * @return 만료 여부
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Refresh Token 갱신 (만료 시간 업데이트)
     * @param newExpiryDate 새로운 만료 시간
     */
    public void updateExpiryDate(LocalDateTime newExpiryDate) {
        this.expiryDate = newExpiryDate;
    }
}

