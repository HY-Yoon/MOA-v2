package com.moa2.api.user.controller;

import com.moa2.api.auth.dto.AuthDto;
import com.moa2.api.user.dto.UserDto;
import com.moa2.api.user.service.UserService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 마이페이지 - 회원 관리 API
 * Cookie 기반 인증 사용
 */
@Slf4j
@Tag(name = "마이페이지 - 회원 관리", description = "내 정보 조회 및 회원 탈퇴 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     * Cookie의 accessToken으로 자동 인증
     */
    @Operation(
        summary = "내 정보 조회",
        description = "로그인한 사용자의 정보를 조회합니다.\n\n" +
                     "**인증 방식:** Cookie (accessToken)\n\n" +
                     "**응답 정보:**\n" +
                     "- 이메일, 이름, 프로필 이미지\n" +
                     "- 소셜 제공자 정보 (Google, Naver, Kakao)\n" +
                     "- 권한 정보 (USER, ADMIN)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = """
                    {
                      "success": true,
                      "data": {
                        "email": "todayda1006@gmail.com",
                        "name": "오늘다",
                        "picture": "https://lh3.googleusercontent.com/...",
                        "provider": "GOOGLE",
                        "providerId": "123456789",
                        "role": "USER"
                      },
                      "message": null
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 - 로그인 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "인증 실패",
                    value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "인증이 필요합니다."
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDto.UserInfoResponse>> getMyInfo() {
        // SecurityContext에서 인증된 사용자 이메일 가져오기
        String email = getAuthenticatedUserEmail();
        
        AuthDto.UserInfoResponse response = userService.getMyInfo(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 회원 탈퇴
     * Cookie의 accessToken으로 자동 인증
     */
    @Operation(
        summary = "회원 탈퇴",
        description = "로그인한 사용자의 계정을 탈퇴합니다.\n\n" +
                     "**탈퇴 조건:**\n" +
                     "- 진행중인 예매(PENDING, CONFIRMED)가 없어야 함\n\n" +
                     "**처리 과정:**\n" +
                     "1. 진행중인 예매 확인\n" +
                     "2. RefreshToken 무효화\n" +
                     "3. 회원 상태를 DELETED로 변경 (Soft Delete)\n\n" +
                     "**주의:** 탈퇴 후에는 복구할 수 없습니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "탈퇴 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = """
                    {
                      "success": true,
                      "data": {
                        "email": "todayda1006@gmail.com",
                        "message": "회원 탈퇴가 완료되었습니다."
                      },
                      "message": "회원 탈퇴가 완료되었습니다."
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "탈퇴 실패 - 진행중인 예매가 있음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "진행중인 예매 있음",
                    value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "진행중인 예매가 있어 탈퇴할 수 없습니다. 예매를 취소한 후 다시 시도해주세요."
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 - 로그인 필요",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "인증 실패",
                    value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "인증이 필요합니다."
                    }
                    """
                )
            )
        )
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.UserDeleteResponse>> deleteMyAccount() {
        try {
            String email = getAuthenticatedUserEmail();
            
            UserDto.UserDeleteResponse response = userService.deleteMyAccount(email);
            return ResponseEntity.ok(ApiResponse.success(response, response.message()));
            
        } catch (IllegalStateException e) {
            // 진행중인 예매가 있는 경우
            log.warn("회원 탈퇴 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
                    
        } catch (IllegalArgumentException e) {
            // 사용자를 찾을 수 없는 경우
            log.error("사용자 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * SecurityContext에서 인증된 사용자의 이메일을 가져옴
     */
    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() 
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new IllegalStateException("인증이 필요합니다.");
        }
        
        Object principalObj = authentication.getPrincipal();
        if (principalObj instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getEmail();
        }
        return (String) principalObj;
    }
}
