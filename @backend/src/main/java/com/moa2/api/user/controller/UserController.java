package com.moa2.api.user.controller;

import com.moa2.api.auth.dto.AuthDto;
import com.moa2.api.user.controller.docs.UserControllerDocs;
import com.moa2.api.user.dto.UserDto;
import com.moa2.api.user.service.UserService;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserService userService;

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDto.UserInfoResponse>> getMyInfo() {
        String email = getAuthenticatedUserEmail();
        AuthDto.UserInfoResponse response = userService.getMyInfo(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.UserDeleteResponse>> deleteMyAccount() {
        try {
            String email = getAuthenticatedUserEmail();
            UserDto.UserDeleteResponse response = userService.deleteMyAccount(email);
            return ResponseEntity.ok(ApiResponse.success(response, response.message()));

        } catch (IllegalStateException e) {
            log.warn("회원 탈퇴 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.error("사용자 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }


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

        return principalObj.toString();
    }
}