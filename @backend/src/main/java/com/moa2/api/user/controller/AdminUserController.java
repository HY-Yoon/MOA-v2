package com.moa2.api.user.controller;

import com.moa2.api.user.controller.docs.AdminUserControllerDocs;
import com.moa2.api.user.dto.UserDto;
import com.moa2.api.user.service.AdminUserService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.moa2.global.model.Gender;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.model.UserStatus;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController implements AdminUserControllerDocs {

    private final AdminUserService adminUserService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto.UserListResponse>>> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) SocialProvider socialProvider,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // 로직은 그대로 유지
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDto.UserListResponse> pageResult = adminUserService.getUserList(
                keyword, searchType, status, gender, socialProvider, sort, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(pageResult)));
    }

    @Override
    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserDto.UserStatusResponse>> changeUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserDto.UserStatusChangeRequest request) {

        UserDto.UserStatusResponse result = adminUserService.changeUserStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
