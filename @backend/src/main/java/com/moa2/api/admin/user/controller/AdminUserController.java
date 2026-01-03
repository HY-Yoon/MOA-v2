package com.moa2.api.admin.user.controller;

import com.moa2.api.admin.user.dto.UserListResponse;
import com.moa2.api.admin.user.dto.UserStatusChangeRequest;
import com.moa2.api.admin.user.dto.UserStatusResponse;
import com.moa2.api.admin.user.service.AdminUserService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원 관리 API", description = "관리자용 회원 관리 API")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    
    private final AdminUserService adminUserService;
    
    @Operation(
        summary = "회원 목록 조회",
        description = "관리자가 회원 목록을 조회합니다.\n\n" +
                     "**요청 파라미터:**\n" +
                     "- `keyword` (optional): 이름 또는 이메일로 검색\n" +
                     "- `sort` (optional, default: \"createdAt,desc\"): 정렬 기준 (예: \"name,asc\", \"createdAt,desc\")\n" +
                     "- `page` (optional, default: 0): 페이지 번호 (0부터 시작)\n" +
                     "- `size` (optional, default: 20): 페이지 크기\n\n" +
                     "**응답 필드:**\n" +
                     "- `id`: 회원 ID\n" +
                     "- `name`: 이름\n" +
                     "- `email`: 이메일\n" +
                     "- `phone`: 연락처 (null 가능)\n" +
                     "- `gender`: 성별 (MALE, FEMALE, OTHER)\n" +
                     "- `socialProvider`: 소셜 로그인 제공자 (KAKAO, NAVER, GOOGLE)\n" +
                     "- `status`: 회원 상태 (ACTIVE, DELETED, SUSPENDED)\n" +
                     "- `isVerified`: 본인인증 여부\n" +
                     "- `createdAt`: 가입일시"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserListResponse>>> getUserList(
            @Parameter(description = "검색 키워드 (이름 또는 이메일)", example = "홍길동")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "정렬 기준 (field,direction)", example = "createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserListResponse> pageResult = adminUserService.getUserList(keyword, sort, pageable);
        PageResponse<UserListResponse> result = PageResponse.of(pageResult);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @Operation(
        summary = "회원 상태 변경",
        description = "관리자가 회원 상태를 변경합니다.\n\n" +
                     "**상태 변경:**\n" +
                     "- `ACTIVE`: 계정 활성화 (suspensionReason, suspendedAt 초기화)\n" +
                     "- `SUSPENDED`: 계정 정지 (reason 필수)\n\n" +
                     "**요청 필드:**\n" +
                     "- `status` (필수): ACTIVE 또는 SUSPENDED\n" +
                     "- `reason` (선택): 정지 사유 (SUSPENDED일 때만 필요)\n\n" +
                     "**응답 필드:**\n" +
                     "- `id`: 회원 ID\n" +
                     "- `name`: 이름\n" +
                     "- `email`: 이메일\n" +
                     "- `status`: 변경된 상태\n" +
                     "- `suspensionReason`: 정지 사유 (null 가능)\n" +
                     "- `updatedAt`: 수정일시"
    )
    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserStatusResponse>> changeUserStatus(
            @Parameter(description = "회원 ID", example = "1", required = true)
            @PathVariable Long userId,
            @Parameter(description = "상태 변경 요청", required = true)
            @Valid @RequestBody UserStatusChangeRequest request) {
        
        UserStatusResponse result = adminUserService.changeUserStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

