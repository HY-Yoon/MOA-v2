package com.moa2.api.auth.controller;

import com.moa2.api.auth.controller.docs.AuthControllerDocs;
import com.moa2.api.auth.dto.AuthDto;
import com.moa2.api.auth.service.RefreshTokenService;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.JwtTokenProvider;
import com.moa2.global.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Value("${security.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${security.cookie.same-site:None}")
    private String cookieSameSite;

    // -------------------------------------------------------------------------
    // JSON API Implementations (from AuthControllerDocs)
    // -------------------------------------------------------------------------

    // @Override
    // @GetMapping("/user")
    // public ResponseEntity<ApiResponse<AuthDto.UserInfoResponse>> getCurrentUser(
    // @AuthenticationPrincipal OAuth2User oAuth2User,
    // HttpServletRequest request) {
    //
    // String email = null;
    //
    // // 1. OAuth2 세션 인증 확인
    // if (oAuth2User != null) {
    // email = oAuth2User.getAttribute("email");
    // log.debug("OAuth2 세션 인증: {}", email);
    // }
    //
    // // 2. JWT 인증 확인 (Cookie에서 자동으로 JwtAuthenticationFilter가 처리)
    // if (email == null) {
    // org.springframework.security.core.Authentication authentication =
    // org.springframework.security.core.context.SecurityContextHolder
    // .getContext().getAuthentication();
    //
    // if (authentication != null && authentication.isAuthenticated()
    // && !(authentication instanceof
    // org.springframework.security.authentication.AnonymousAuthenticationToken)) {
    // Object principalObj = authentication.getPrincipal();
    // if (principalObj instanceof UserPrincipal userPrincipal) {
    // email = userPrincipal.getEmail();
    // } else {
    // email = (String) principalObj;
    // }
    // log.debug("JWT 인증 (Cookie): {}", email);
    // }
    // }
    //
    // // 3. 이메일이 없으면 인증 실패
    // if (email == null || email.isEmpty()) {
    // log.warn("인증되지 않은 요청: /api/auth/user");
    // return ResponseEntity.status(401)
    // .body(ApiResponse.error("인증이 필요합니다."));
    // }
    //
    // // 4. Access Token에서 provider 정보 추출 및 사용자 정보 조회
    // User user = null;
    // String accessToken = getAccessTokenFromCookie(request);
    //
    // if (accessToken != null) {
    // try {
    // String providerStr =
    // jwtTokenProvider.getProviderFromAccessToken(accessToken);
    // if (providerStr != null && !providerStr.isEmpty()) {
    // try {
    // SocialProvider socialProvider = SocialProvider.valueOf(providerStr);
    // user = userRepository.findByEmailAndSocialProvider(email,
    // socialProvider).orElse(null);
    // } catch (IllegalArgumentException e) {
    // log.warn("알 수 없는 provider 값: {}", providerStr);
    // }
    // }
    // } catch (Exception e) {
    // log.warn("토큰에서 provider 추출 실패: {}", e.getMessage());
    // }
    // }
    //
    // if (user == null) {
    // user = userRepository.findByEmail(email).orElse(null);
    // }
    //
    // if (user == null) {
    // return ResponseEntity.status(404)
    // .body(ApiResponse.error("사용자를 찾을 수 없습니다."));
    // }
    //
    // return
    // ResponseEntity.ok(ApiResponse.success(AuthDto.UserInfoResponse.from(user)));
    // }

//    @Override
//    @GetMapping("/verify")
//    public ResponseEntity<ApiResponse<AuthDto.TokenVerifyResponse>> verifyToken(@RequestParam String token) {
//
//        if (!jwtTokenProvider.validateAccessToken(token)) {
//            return ResponseEntity.status(401)
//                    .body(ApiResponse.success(AuthDto.TokenVerifyResponse.fail("유효하지 않은 토큰입니다.")));
//        }
//
//        String email = jwtTokenProvider.getEmailFromAccessToken(token);
//        User user = null;
//
//        try {
//            String providerStr = jwtTokenProvider.getProviderFromAccessToken(token);
//            if (providerStr != null && !providerStr.isEmpty()) {
//                try {
//                    SocialProvider socialProvider = SocialProvider.valueOf(providerStr);
//                    user = userRepository.findByEmailAndSocialProvider(email, socialProvider).orElse(null);
//                } catch (IllegalArgumentException e) {
//                    log.warn("알 수 없는 provider 값: {}", providerStr);
//                }
//            }
//        } catch (Exception e) {
//            log.warn("토큰에서 provider 추출 실패: {}", e.getMessage());
//        }
//
//        if (user == null) {
//            user = userRepository.findByEmail(email).orElse(null);
//        }
//
//        if (user == null) {
//            return ResponseEntity.status(404)
//                    .body(ApiResponse.success(AuthDto.TokenVerifyResponse.fail("사용자를 찾을 수 없습니다.")));
//        }
//
//        return ResponseEntity
//                .ok(ApiResponse.success(AuthDto.TokenVerifyResponse.success(AuthDto.UserInfoResponse.from(user))));
//    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = getRefreshTokenFromCookie(request);

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Refresh Token이 없습니다."));
        }

        AuthDto.TokenResponse tokenResponse = refreshTokenService.refreshAccessToken(refreshToken);

        jakarta.servlet.http.Cookie accessTokenCookie = new jakarta.servlet.http.Cookie("accessToken",
                tokenResponse.accessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false); // SSL 설정 전까지 false
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(30 * 60); // 30분
        response.addCookie(accessTokenCookie);

        log.info("Access Token 갱신 완료");

        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    // -------------------------------------------------------------------------
    // API Methods
    // -------------------------------------------------------------------------

    @Override
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = getRefreshTokenFromCookie(request);

            if (refreshToken != null) {
                try {
                    if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
                        String email = jwtTokenProvider.getEmailFromRefreshToken(refreshToken);
                        String providerStr = jwtTokenProvider.getProviderFromRefreshToken(refreshToken);

                        if (providerStr != null && !providerStr.isEmpty()) {
                            try {
                                SocialProvider socialProvider = SocialProvider.valueOf(providerStr);
                                refreshTokenService.deleteByUserEmailAndSocialProvider(email, socialProvider);
                                log.info("로그아웃 완료: {} ({})", email, socialProvider);
                            } catch (IllegalArgumentException e) {
                                refreshTokenService.deleteByUserEmail(email);
                            }
                        } else {
                            refreshTokenService.deleteByUserEmail(email);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Refresh Token 처리 중 오류: {}", e.getMessage());
                }
            }

            // Cross-Origin 환경에서 확실한 쿠키 삭제를 위해 ResponseCookie 사용
            ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "")
                    .path("/")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite)
                    .maxAge(0) // 즉시 만료
                    .build();
            response.addHeader("Set-Cookie", accessTokenCookie.toString());

            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
                    .path("/")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite)
                    .maxAge(0) // 즉시 만료
                    .build();
            response.addHeader("Set-Cookie", refreshTokenCookie.toString());

            // JSESSIONID 쿠키 삭제 추가 (OAuth2 세션 잔류 방지)
            ResponseCookie jsessionidCookie = ResponseCookie.from("JSESSIONID", "")
                    .path("/")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite)
                    .maxAge(0)
                    .build();
            response.addHeader("Set-Cookie", jsessionidCookie.toString());

            // 세션 무효화 및 SecurityContext 초기화
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            org.springframework.security.core.context.SecurityContextHolder.clearContext();

            log.info("쿠키 및 세션 삭제 완료");

        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("로그아웃 중 오류가 발생했습니다."));
        }

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
