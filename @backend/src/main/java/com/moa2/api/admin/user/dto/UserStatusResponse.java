package com.moa2.api.admin.user.dto;

import com.moa2.global.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusResponse {
    private Long id;
    private String name;
    private String email;
    private UserStatus status;
    private String suspensionReason; // null 가능
    private LocalDateTime updatedAt;
}

