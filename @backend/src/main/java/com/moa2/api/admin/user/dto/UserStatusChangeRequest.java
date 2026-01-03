package com.moa2.api.admin.user.dto;

import com.moa2.global.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원 상태 변경 요청")
public class UserStatusChangeRequest {
    
    @Schema(description = "회원 상태", example = "SUSPENDED", allowableValues = {"ACTIVE", "SUSPENDED"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "상태는 필수입니다")
    private UserStatus status;
    
    @Schema(description = "정지 사유 (SUSPENDED일 때만 필요)", example = "부적절한 행위로 인한 정지")
    private String reason;
}

