package com.moa2.domain.user.entity;

import com.moa2.global.model.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_email_provider", columnNames = {"email", "social_provider"})
})
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider socialProvider; // KAKAO, NAVER, GOOGLE

    private String providerId;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Enumerated(EnumType.STRING)
    private Gender gender; // MALE, FEMALE, OTHER

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private UserRole role; // USER, ADMIN

    private Boolean isVerified; // 본인인증 여부

    @Enumerated(EnumType.STRING)
    private UserStatus status; // ACTIVE, DELETED, SUSPENDED

    private String suspensionReason;
    private LocalDateTime suspendedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Builder
    public User(String email, SocialProvider socialProvider, String providerId, String name) {
        this.email = email;
        this.socialProvider = socialProvider;
        this.providerId = providerId;
        this.name = name;
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
        this.isVerified = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 본인 인증 성공 시 정보 업데이트 로직
    public void verifyUser(String name, String phone, LocalDate birthDate, Gender gender) {
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.isVerified = true;
        this.updatedAt = LocalDateTime.now();
    }

    // 회원 탈퇴 (Soft Delete)
    public void withdraw() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 계정 정지
    public void suspend(String reason) {
        this.status = UserStatus.SUSPENDED;
        this.suspensionReason = reason;
        this.suspendedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 계정 활성화
    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.suspensionReason = null;
        this.suspendedAt = null;
        this.updatedAt = LocalDateTime.now();
    }
}







