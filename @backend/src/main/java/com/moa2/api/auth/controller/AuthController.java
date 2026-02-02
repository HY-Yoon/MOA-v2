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

    // -------------------------------------------------------------------------
    // JSON API Implementations (from AuthControllerDocs)
    // -------------------------------------------------------------------------

    @Override
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<AuthDto.UserInfoResponse>> getCurrentUser(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            HttpServletRequest request) {

        String email = null;

        // 1. OAuth2 세션 인증 확인
        if (oAuth2User != null) {
            email = oAuth2User.getAttribute("email");
            log.debug("OAuth2 세션 인증: {}", email);
        }

        // 2. JWT 인증 확인 (Cookie에서 자동으로 JwtAuthenticationFilter가 처리)
        if (email == null) {
            org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                Object principalObj = authentication.getPrincipal();
                if (principalObj instanceof UserPrincipal userPrincipal) {
                    email = userPrincipal.getEmail();
                } else {
                    email = (String) principalObj;
                }
                log.debug("JWT 인증 (Cookie): {}", email);
            }
        }

        // 3. 이메일이 없으면 인증 실패
        if (email == null || email.isEmpty()) {
            log.warn("인증되지 않은 요청: /api/auth/user");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증이 필요합니다."));
        }

        // 4. Access Token에서 provider 정보 추출 및 사용자 정보 조회
        User user = null;
        String accessToken = getAccessTokenFromCookie(request);

        if (accessToken != null) {
            try {
                String providerStr = jwtTokenProvider.getProviderFromAccessToken(accessToken);
                if (providerStr != null && !providerStr.isEmpty()) {
                    try {
                        SocialProvider socialProvider = SocialProvider.valueOf(providerStr);
                        user = userRepository.findByEmailAndSocialProvider(email, socialProvider).orElse(null);
                    } catch (IllegalArgumentException e) {
                        log.warn("알 수 없는 provider 값: {}", providerStr);
                    }
                }
            } catch (Exception e) {
                log.warn("토큰에서 provider 추출 실패: {}", e.getMessage());
            }
        }

        if (user == null) {
            user = userRepository.findByEmail(email).orElse(null);
        }

        if (user == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("사용자를 찾을 수 없습니다."));
        }

        return ResponseEntity.ok(ApiResponse.success(AuthDto.UserInfoResponse.from(user)));
    }

    @Override
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<AuthDto.TokenVerifyResponse>> verifyToken(@RequestParam String token) {

        if (!jwtTokenProvider.validateAccessToken(token)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.success(AuthDto.TokenVerifyResponse.fail("유효하지 않은 토큰입니다.")));
        }

        String email = jwtTokenProvider.getEmailFromAccessToken(token);
        User user = null;

        try {
            String providerStr = jwtTokenProvider.getProviderFromAccessToken(token);
            if (providerStr != null && !providerStr.isEmpty()) {
                try {
                    SocialProvider socialProvider = SocialProvider.valueOf(providerStr);
                    user = userRepository.findByEmailAndSocialProvider(email, socialProvider).orElse(null);
                } catch (IllegalArgumentException e) {
                    log.warn("알 수 없는 provider 값: {}", providerStr);
                }
            }
        } catch (Exception e) {
            log.warn("토큰에서 provider 추출 실패: {}", e.getMessage());
        }

        if (user == null) {
            user = userRepository.findByEmail(email).orElse(null);
        }

        if (user == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.success(AuthDto.TokenVerifyResponse.fail("사용자를 찾을 수 없습니다.")));
        }

        return ResponseEntity
                .ok(ApiResponse.success(AuthDto.TokenVerifyResponse.success(AuthDto.UserInfoResponse.from(user))));
    }

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
    // HTML Methods (Keep as is)
    // -------------------------------------------------------------------------

    @GetMapping("/success")
    public String success(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        String accessToken = getAccessTokenFromCookie(request);
        String refreshToken = getRefreshTokenFromCookie(request);

        if (accessToken == null || refreshToken == null) {
            return generateErrorHtml("토큰을 찾을 수 없습니다. 다시 로그인해주세요.", null);
        }

        String email = null;
        String socialProvider = "Unknown";

        try {
            if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
                email = jwtTokenProvider.getEmailFromRefreshToken(refreshToken);
                String providerStr = jwtTokenProvider.getProviderFromRefreshToken(refreshToken);

                SocialProvider socialProviderEnum = null;
                if (providerStr != null && !providerStr.isEmpty()) {
                    try {
                        socialProviderEnum = SocialProvider.valueOf(providerStr);
                        socialProvider = switch (socialProviderEnum) {
                            case GOOGLE -> "Google";
                            case NAVER -> "Naver";
                            case KAKAO -> "Kakao";
                        };
                    } catch (IllegalArgumentException e) {
                        log.warn("알 수 없는 provider 값: {}", providerStr);
                    }
                }

                User user = null;
                if (socialProviderEnum != null) {
                    user = userRepository.findByEmailAndSocialProvider(email, socialProviderEnum).orElse(null);
                } else {
                    user = userRepository.findByEmail(email).orElse(null);
                    if (user != null) {
                        socialProvider = switch (user.getSocialProvider()) {
                            case GOOGLE -> "Google";
                            case NAVER -> "Naver";
                            case KAKAO -> "Kakao";
                        };
                    }
                }
            }
        } catch (Exception e) {
            log.error("토큰 검증 중 오류: {}", e.getMessage());
            return generateErrorHtml("토큰 검증에 실패했습니다. 다시 로그인해주세요.", null);
        }

        if (email == null) {
            return generateErrorHtml("사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.", null);
        }

        return generateSuccessHtml(accessToken, refreshToken, email, socialProvider);
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
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

            jakarta.servlet.http.Cookie accessTokenCookie = new jakarta.servlet.http.Cookie("accessToken", null);
            accessTokenCookie.setMaxAge(0);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setHttpOnly(true);
            response.addCookie(accessTokenCookie);

            jakarta.servlet.http.Cookie refreshTokenCookie = new jakarta.servlet.http.Cookie("refreshToken", null);
            refreshTokenCookie.setMaxAge(0);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setHttpOnly(true);
            response.addCookie(refreshTokenCookie);

            log.info("쿠키 삭제 완료");

        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생: {}", e.getMessage(), e);
        }
        return generateLoginHtml();
    }

    @GetMapping("/login")
    public String login() {
        return generateLoginHtml();
    }

    @GetMapping("/error")
    public String error(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String code) {
        if (message == null || message.isEmpty()) {
            message = "알 수 없는 오류가 발생했습니다.";
        }
        return generateErrorHtml(message, code);
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private String getAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

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

    private String generateSuccessHtml(String accessToken, String refreshToken, String email, String socialProvider) {
        long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpiration();
        long refreshTokenExpiresIn = jwtTokenProvider.getRefreshTokenExpiration();
        String accessTokenExpiresInText = formatExpirationTime(accessTokenExpiresIn);
        String refreshTokenExpiresInText = formatExpirationTime(refreshTokenExpiresIn);

        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>로그인 성공 - MOA2</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            padding: 20px;
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 12px;
                            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                            max-width: 800px;
                            width: 100%%;
                        }
                        h1 {
                            color: #333;
                            margin-bottom: 10px;
                            text-align: center;
                        }
                        .subtitle {
                            color: #666;
                            text-align: center;
                            margin-bottom: 30px;
                        }
                        .token-section {
                            margin-bottom: 30px;
                        }
                        .token-section h3 {
                            color: #333;
                            margin-bottom: 10px;
                            font-size: 18px;
                        }
                        .expiry-info {
                            color: #666;
                            font-size: 14px;
                            margin-bottom: 8px;
                        }
                        label {
                            display: block;
                            margin-bottom: 8px;
                            color: #555;
                            font-weight: 600;
                        }
                        textarea {
                            width: 100%%;
                            padding: 12px;
                            border: 2px solid #e0e0e0;
                            border-radius: 8px;
                            font-family: 'Courier New', monospace;
                            font-size: 11px;
                            resize: vertical;
                            min-height: 80px;
                            box-sizing: border-box;
                        }
                        textarea:focus {
                            outline: none;
                            border-color: #667eea;
                        }
                        .button-group {
                            display: flex;
                            gap: 10px;
                            margin-top: 15px;
                        }
                        button {
                            flex: 1;
                            padding: 12px 24px;
                            border: none;
                            border-radius: 8px;
                            font-size: 16px;
                            font-weight: 600;
                            cursor: pointer;
                            transition: all 0.3s;
                        }
                        .copy-btn {
                            background: #667eea;
                            color: white;
                        }
                        .copy-btn:hover {
                            background: #5568d3;
                            transform: translateY(-2px);
                            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
                        }
                        .copy-btn:active {
                            transform: translateY(0);
                        }
                        .refresh-btn {
                            background: #28a745;
                            color: white;
                        }
                        .refresh-btn:hover {
                            background: #218838;
                            transform: translateY(-2px);
                            box-shadow: 0 4px 12px rgba(40, 167, 69, 0.4);
                        }
                        .logout-btn {
                            background: #dc3545;
                            color: white;
                        }
                        .logout-btn:hover {
                            background: #c82333;
                            transform: translateY(-2px);
                            box-shadow: 0 4px 12px rgba(220, 53, 69, 0.4);
                        }
                        .info-section {
                            margin-top: 30px;
                            padding: 20px;
                            background: #f5f5f5;
                            border-radius: 8px;
                        }
                        .info-item {
                            margin-bottom: 10px;
                            color: #555;
                        }
                        .info-label {
                            font-weight: 600;
                            color: #333;
                        }
                        .success-message {
                            background: #d4edda;
                            color: #155724;
                            padding: 12px;
                            border-radius: 8px;
                            margin-bottom: 20px;
                            text-align: center;
                        }
                        .divider {
                            height: 2px;
                            background: #e0e0e0;
                            margin: 30px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>✅ 로그인 성공!</h1>
                        <p class="subtitle">Access Token과 Refresh Token이 발급되었습니다.</p>

                        <div class="success-message">
                            🎉 %s 소셜 로그인이 완료되었습니다.
                        </div>

                        <div class="token-section">
                            <h3>🔑 Access Token</h3>
                            <div class="expiry-info">⏰ 만료 시간: %s</div>
                            <label for="access-token">Access Token:</label>
                            <textarea id="access-token" readonly>%s</textarea>
                            <div class="button-group">
                                <button class="copy-btn" onclick="copyToken('access-token', this)">📋 Access Token 복사</button>
                            </div>
                        </div>

                        <div class="divider"></div>

                        <div class="token-section">
                            <h3>🔄 Refresh Token</h3>
                            <div class="expiry-info">⏰ 만료 시간: %s</div>
                            <label for="refresh-token">Refresh Token:</label>
                            <textarea id="refresh-token" readonly>%s</textarea>
                            <div class="button-group">
                                <button class="copy-btn" onclick="copyToken('refresh-token', this)">📋 Refresh Token 복사</button>
                                <button class="refresh-btn" onclick="refreshAccessToken()">🔄 Access Token 갱신 테스트</button>
                            </div>
                        </div>

                        <div class="divider"></div>

                        <div class="token-section">
                            <h3>🧪 Cookie 기반 API 테스트</h3>
                            <div class="button-group">
                                <button class="copy-btn" onclick="testGetUser()">👤 사용자 정보 조회</button>
                                <button class="logout-btn" onclick="logout()">🚪 로그아웃</button>
                            </div>
                        </div>

                        <div class="info-section">
                            <div class="info-item">
                                <span class="info-label">이메일:</span> %s
                            </div>
                            <div class="info-item">
                                <span class="info-label">🍪 Cookie 기반 인증:</span> Access Token과 Refresh Token이 HttpOnly Cookie로 저장되어 자동으로 전송됩니다.
                            </div>
                            <div class="info-item">
                                <span class="info-label">토큰 갱신:</span> Access Token이 만료되면 "Access Token 갱신 테스트" 버튼을 클릭하세요.
                            </div>
                            <div class="info-item">
                                <span class="info-label">쿠키 확인:</span> F12 → Application 탭 → Cookies → http://localhost:8081 에서 확인하세요.
                            </div>
                        </div>
                    </div>

                    <script>
                        // 이메일을 JavaScript 변수로 저장
                        const userEmail = '%s';

                        function copyToken(textareaId, btn) {
                            const tokenTextarea = document.getElementById(textareaId);
                            tokenTextarea.select();
                            tokenTextarea.setSelectionRange(0, 99999);

                            try {
                                document.execCommand('copy');
                                const originalText = btn.textContent;
                                btn.textContent = '✅ 복사 완료!';
                                btn.style.background = '#28a745';

                                setTimeout(() => {
                                    btn.textContent = originalText;
                                    btn.style.background = '#667eea';
                                }, 2000);
                            } catch (err) {
                                alert('복사 실패: ' + err);
                            }
                        }

                        async function testGetUser() {
                            try {
                                // Cookie에서 자동으로 Access Token을 읽어서 사용자 정보 조회
                                const response = await fetch('/api/auth/user', {
                                    method: 'GET',
                                    credentials: 'include'  // 🍪 Cookie 자동 전송!
                                });

                                if (response.ok) {
                                    const result = await response.json();
                                    if (result.success && result.data) {
                                        const user = result.data;
                                        alert('✅ 사용자 정보 조회 성공!\\n\\n' +
                                            '이메일: ' + user.email + '\\n' +
                                            '이름: ' + user.name + '\\n' +
                                            '소셜 제공자: ' + user.provider + '\\n' +
                                            '권한: ' + user.role);
                                    }
                                } else {
                                    const error = await response.json();
                                    alert('❌ 사용자 정보 조회 실패: ' + (error.message || '알 수 없는 오류'));
                                }
                            } catch (err) {
                                alert('사용자 정보 조회 중 오류 발생: ' + err.message);
                            }
                        }

                        async function refreshAccessToken() {
                            try {
                                // Cookie에서 자동으로 Refresh Token을 읽어서 갱신
                                const response = await fetch('/api/auth/refresh', {
                                    method: 'POST',
                                    credentials: 'include'  // 🍪 Cookie 자동 전송!
                                });

                                if (response.ok) {
                                    const result = await response.json();
                                    // 새로운 Access Token을 화면에 표시
                                    if (result.success && result.data) {
                                        document.getElementById('access-token').value = result.data.accessToken;
                                        alert('✅ Access Token이 갱신되었습니다!\\n\\n새 토큰이 Cookie에 저장되었습니다.\\nF12 → Application → Cookies에서 확인하세요.');
                                    }
                                } else {
                                    const error = await response.json();
                                    alert('❌ 토큰 갱신 실패: ' + (error.message || error.error || '알 수 없는 오류'));
                                }
                            } catch (err) {
                                alert('토큰 갱신 중 오류 발생: ' + err.message);
                            }
                        }

                        async function logout() {
                            if (confirm('로그아웃하시겠습니까?')) {
                                try {
                                    // Cookie에서 자동으로 Refresh Token을 읽어서 로그아웃
                                    const response = await fetch('/api/auth/logout', {
                                        method: 'POST',
                                        credentials: 'include'  // 🍪 Cookie 자동 전송!
                                    });

                                    if (response.ok) {
                                        alert('✅ 로그아웃 완료!\\n\\nCookie가 삭제되었습니다.');
                                        // 로그인 페이지로 리다이렉트
                                        window.location.href = '/api/auth/login';
                                    } else {
                                        alert('❌ 로그아웃 실패. 다시 시도해주세요.');
                                    }
                                } catch (err) {
                                    alert('로그아웃 중 오류 발생: ' + err.message);
                                }
                            }
                        }
                    </script>
                </body>
                </html>
                """
                .formatted(
                        socialProvider, // 소셜 제공자 이름
                        accessTokenExpiresInText, accessToken,
                        refreshTokenExpiresInText, refreshToken,
                        email, // HTML 표시용
                        email // JavaScript 변수용
                );
    }

    private String formatExpirationTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "일";
        } else if (hours > 0) {
            return hours + "시간";
        } else if (minutes > 0) {
            return minutes + "분";
        } else {
            return seconds + "초";
        }
    }

    private String generateLoginHtml() {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
                    <meta http-equiv="Pragma" content="no-cache">
                    <meta http-equiv="Expires" content="0">
                    <title>로그인 - MOA2</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            padding: 20px;
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 12px;
                            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                            max-width: 500px;
                            width: 100%%;
                            text-align: center;
                        }
                        h1 {
                            color: #333;
                            margin-bottom: 10px;
                        }
                        .subtitle {
                            color: #666;
                            margin-bottom: 30px;
                        }
                        .btn {
                            display: block;
                            width: 100%%;
                            padding: 12px;
                            margin: 10px 0;
                            border: none;
                            border-radius: 8px;
                            font-size: 16px;
                            font-weight: 600;
                            cursor: pointer;
                            text-decoration: none;
                            transition: transform 0.2s, box-shadow 0.2s;
                            box-sizing: border-box;
                        }
                        .btn:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                        }
                        .btn-google {
                            background-color: #ffffff;
                            color: #757575;
                            border: 1px solid #ddd;
                        }
                        .btn-naver {
                            background-color: #03C75A;
                            color: white;
                        }
                        .btn-kakao {
                            background-color: #FEE500;
                            color: #000000;
                        }
                        .admin-link {
                            margin-top: 20px;
                            display: inline-block;
                            color: #666;
                            text-decoration: none;
                            font-size: 14px;
                        }
                        .admin-link:hover {
                            text-decoration: underline;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🔐 로그인</h1>
                        <p class="subtitle">MOA2 서비스에 오신 것을 환영합니다.</p>

                        <a href="/oauth2/authorization/google" class="btn btn-google">Google로 계속하기</a>
                        <a href="/oauth2/authorization/naver" class="btn btn-naver">Naver로 계속하기</a>
                        <a href="/oauth2/authorization/kakao" class="btn btn-kakao">Kakao로 계속하기</a>

                        <br>
                        <a href="/admin/login" class="admin-link">관리자 로그인</a>
                    </div>
                </body>
                </html>
                """;
    }

    private String generateErrorHtml(String message, String code) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <title>오류 발생 - MOA2</title>
                    <style>
                        body { font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #f8f9fa; }
                        .container { background: white; padding: 40px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); text-align: center; max-width: 500px; width: 100%%; }
                        h1 { color: #dc3545; margin-bottom: 20px; }
                        p { color: #555; font-size: 16px; margin-bottom: 30px; }
                        .btn { background: #667eea; color: white; border: none; padding: 10px 20px; border-radius: 4px; cursor: pointer; text-decoration: none; font-size: 14px; }
                        .code { color: #999; font-size: 12px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>⚠️ 오류 발생</h1>
                        <p>%s</p>
                        <a href="/api/auth/login" class="btn">로그인 페이지로 돌아가기</a>
                        <div class="code">%s</div>
                    </div>
                </body>
                </html>
                """
                .formatted(message, code != null ? "Code: " + code : "");
    }
}
