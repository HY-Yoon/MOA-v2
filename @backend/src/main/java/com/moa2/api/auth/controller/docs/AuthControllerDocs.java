package com.moa2.api.auth.controller.docs;

import com.moa2.api.auth.dto.AuthDto;
import com.moa2.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "인증 API", description = "로그인, 토큰 검증, 재발급 등 인증 관련 API")
public interface AuthControllerDocs {

    @Operation(summary = "Auth Code → JWT 토큰 교환", description = """
            OAuth2 소셜 로그인 성공 후 발급된 일회용 인증 코드(code)를 실제 JWT 토큰으로 교환합니다.

            **처리 내용:**
            1. 요청 바디의 code를 Redis에서 조회하여 유효성 검사
            2. 유효한 경우 accessToken, refreshToken 발급 및 RefreshToken DB 저장
            3. 사용된 code는 즉시 Redis에서 삭제 (일회용)

            **성공 시:** accessToken, refreshToken이 JSON Body로 반환됩니다.
            프론트엔드는 이 값을 자신의 도메인에서 HttpOnly 쿠키로 저장해야 합니다.

            **Postman 테스트 방법:**
            1. 브라우저에서 소셜 로그인 수행 후 콜백 URL의 ?code=... 값을 복사
            2. POST /api/auth/exchange-code 요청 바디에 {"code": "복사한값"} 전송
            """)
    ResponseEntity<ApiResponse<AuthDto.TokenResponse>> exchangeCode(
            AuthDto.ExchangeCodeRequest request);

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

    @Operation(summary = "로그아웃", description = """
            사용자를 로그아웃 처리하고 Access Token 및 Refresh Token 쿠키를 삭제합니다.

            **처리 내용:**
            1. Cookie에서 Refresh Token 추출 확인
            2. DB에서 해당 로그인 정보(Refresh Token) 삭제
            3. Access Token 및 Refresh Token Cookie 강제 만료 처리 (SameSite=None 설정)

            **성공 시:** 쿠키가 삭제되고 200 OK 빈 응답이 반환됩니다.
            """)
    ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response);
}
