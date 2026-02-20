package com.moa2.api.auth.controller.docs;

import com.moa2.api.auth.dto.AuthDto;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "인증 API", description = "로그인, 토큰 검증, 재발급 등 인증 관련 API")
public interface AuthControllerDocs {

//    @Operation(summary = "현재 로그인한 사용자 정보 조회", description = """
//            현재 로그인한 사용자의 정보를 조회합니다.
//            JWT 인증 (Cookie) 또는 OAuth2 세션 인증을 지원합니다.
//
//            **반환 정보:**
//            - 이름, 이메일, 프로필 사진, 소셜 제공자 정보 등
//
//            **권한:** 인증된 사용자만 가능
//            """)
//    ResponseEntity<ApiResponse<AuthDto.UserInfoResponse>> getCurrentUser(
//            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oAuth2User,
//            HttpServletRequest request);

    @Operation(summary = "JWT 토큰 검증", description = """
            Access Token의 유효성을 검증하고 사용자 정보를 반환합니다.

            **처리 내용:**
            1. 토큰 서명 및 만료 여부 검증
            2. 토큰 내 사용자 정보(이메일, 권한) 추출

            **반환:**
            - 유효한 경우: valid=true, 사용자 정보 포함
            - 유효하지 않은 경우: valid=false, 에러 메시지 포함
            """)
    ResponseEntity<ApiResponse<AuthDto.TokenVerifyResponse>> verifyToken(
            @Parameter(description = "Access Token", required = true) @RequestParam String token);

    @Operation(summary = "Access Token 갱신", description = """
            Refresh Token을 사용하여 새로운 Access Token을 발급합니다.
            Refresh Token은 HttpOnly Cookie에서 자동으로 추출합니다.

            **처리 내용:**
            1. Cookie에서 Refresh Token 추출 확인
            2. Refresh Token 유효성 및 DB 저장 여부 검증
            3. 새로운 Access Token 발급 및 Cookie 재설정

            **성공 시:** 새로운 Access Token이 Cookie에 설정되고 정보가 반환됩니다.
            """)
    ResponseEntity<ApiResponse<AuthDto.TokenResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response);
}
