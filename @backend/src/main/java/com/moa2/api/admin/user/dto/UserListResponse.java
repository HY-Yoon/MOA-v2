package com.moa2.api.admin.user.dto;

import com.moa2.global.model.SocialProvider;
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
public class UserListResponse {
    private Long id;
    private String name;
    private String email;
    private String phone; // null 가능
    private String gender; // MALE, FEMALE, OTHER
    private SocialProvider socialProvider;
    private UserStatus status;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}

