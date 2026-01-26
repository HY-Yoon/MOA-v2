package com.moa2.api.user.dto;

import com.moa2.api.user.domain.entity.User;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class UserDto {

    /**
     * 회원 탈퇴 응답 DTO
     */
    public record UserDeleteResponse (
        String email,
        String message
    ){ }

    /**
     * 관리자 리스트 응답 DTO
     */
    public record UserListResponse (
        Long id,
        String name,
        String email,
        String phone, // null 가능
        String gender, // MALE, FEMALE, OTHER
        SocialProvider socialProvider,
        UserStatus status,
        Boolean isVerified,
        LocalDateTime createdAt) {


        public static UserListResponse from(User user) {
            return new UserListResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getGender() != null ? user.getGender().name() : null, // Enum 처리
                    user.getSocialProvider(),
                    user.getStatus(),
                    user.getIsVerified(),
                    user.getCreatedAt()
            );
        }
    }

    /**
     * 관리자 회원 상태 변경 DTO
     */
    @Schema(description = "회원 상태 변경 요청")
    public record UserStatusChangeRequest(

            @Schema(description = "회원 상태", example = "SUSPENDED", allowableValues = {"ACTIVE", "SUSPENDED"}, requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "상태는 필수입니다")
            UserStatus status,

            @Schema(description = "정지 사유 (SUSPENDED일 때만 필요)", example = "부적절한 행위로 인한 정지")
            String reason
    ) {}

    /**
     * 관리자 회원 상태 조회 DTO
     */
    public record UserStatusResponse (
        Long id,
        String name,
        String email,
        UserStatus status,
        String suspensionReason, // null 가능

        LocalDateTime updatedAt
    ){
        public static UserStatusResponse from(User user) {
            return new UserStatusResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getStatus(),
                    user.getSuspensionReason(),
                    user.getUpdatedAt()
            );
        }
    }


}
