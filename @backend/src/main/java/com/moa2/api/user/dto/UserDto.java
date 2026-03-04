package com.moa2.api.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moa2.api.user.domain.entity.User;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.model.UserRole;
import com.moa2.global.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

public class UserDto {

        @Builder
        @Schema(description = "회원 탈퇴 응답")
        public record UserDeleteResponse(
                        @Schema(description = "탈퇴한 계정 이메일", example = "todayda1006@gmail.com") String email,

                        @Schema(description = "처리 결과 메시지", example = "회원 탈퇴가 완료되었습니다.") String message) {
        }

        /**
         * 관리자 리스트 응답 DTO
         */
        @Builder
        @Schema(description = "관리자용 회원 목록 조회 응답")
        public record UserListResponse(
                        @Schema(description = "회원 ID", example = "1") Long id,

                        @Schema(description = "이름", example = "홍길동") String name,

                        @Schema(description = "이메일", example = "user@example.com") String email,

                        @Schema(description = "전화번호", example = "010-1234-5678") String phone, // null 가능

                        @Schema(description = "성별", example = "MALE", allowableValues = {
                                        "MALE", "FEMALE", "OTHER" }) String gender, // MALE, FEMALE, OTHER (Entity는
                                                                                    // Enum이나 DTO는 String으로 반환 중)

                        @Schema(description = "소셜 제공자", example = "KAKAO") SocialProvider socialProvider,

                        @Schema(description = "회원 상태", example = "ACTIVE") UserStatus status,

                        @Schema(description = "본인인증 여부", example = "true") Boolean isVerified,

                        @Schema(description = "가입일시") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,

                        @Schema(description = "사용자 권한", example = "USER") UserRole role) {
                public static UserListResponse from(User user) {
                        return UserListResponse.builder()
                                        .id(user.getId())
                                        .name(user.getName())
                                        .email(user.getEmail())
                                        .phone(user.getPhone())
                                        .gender(user.getGender() != null ? user.getGender().name() : null) // Enum ->
                                                                                                           // String 변환
                                                                                                           // 유지
                                        .socialProvider(user.getSocialProvider())
                                        .status(user.getStatus())
                                        .isVerified(user.getIsVerified())
                                        .createdAt(user.getCreatedAt())
                                        .role(user.getRole())
                                        .build();
                }
        }

        /**
         * 관리자 회원 상태 변경 DTO
         */
        @Schema(description = "관리자용 회원 상태 변경 요청")
        public record UserStatusChangeRequest(

                        @NotNull(message = "상태는 필수입니다") @Schema(description = "변경할 회원 상태", example = "SUSPENDED", allowableValues = {
                                        "ACTIVE",
                                        "SUSPENDED" }, requiredMode = Schema.RequiredMode.REQUIRED) UserStatus status,

                        @Schema(description = "정지 사유 (SUSPENDED일 때만 필수)", example = "운영 정책 위반으로 인한 정지") String reason) {
        }

        /**
         * 관리자 회원 상태 조회 DTO
         */
        @Builder
        @Schema(description = "관리자용 회원 상태 변경 응답")
        public record UserStatusResponse(
                        @Schema(description = "회원 ID", example = "1") Long id,

                        @Schema(description = "이름", example = "홍길동") String name,

                        @Schema(description = "이메일", example = "user@example.com") String email,

                        @Schema(description = "변경된 상태", example = "SUSPENDED") UserStatus status,

                        @Schema(description = "정지 사유", example = "운영 정책 위반") String suspensionReason, // null 가능

                        @Schema(description = "정보 수정일시") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt) {
                public static UserStatusResponse from(User user) {
                        return UserStatusResponse.builder()
                                        .id(user.getId())
                                        .name(user.getName())
                                        .email(user.getEmail())
                                        .status(user.getStatus())
                                        .suspensionReason(user.getSuspensionReason())
                                        .updatedAt(user.getUpdatedAt())
                                        .build();
                }
        }
}