package com.moa2.api.auth.controller;


import com.moa2.api.auth.dto.AuthDto;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.JwtTokenProvider;
import com.moa2.global.security.UserPrincipal;
import com.moa2.api.auth.service.RefreshTokenService;
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
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    /**
     * OAuth2 로그인 성공 후 JWT 토큰을 표시하는 HTML 페이지
     * 추루 프론트 개발시 응답값 변경
     * 
     * @param request  HTTP 요청 (쿠키에서 토큰 추출용)
     * @param response HTTP 응답 (캐시 방지 헤더 추가용)
     * @return JWT 토큰이 포함된 HTML
     */
    @GetMapping("/success")
    public String success(HttpServletRequest request, HttpServletResponse response) {
        // 캐시 방지 헤더 추가
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // 쿠키에서 토큰 추출
        String accessToken = null;
        String refreshToken = null;
        
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                } else if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        if (accessToken == null || refreshToken == null) {
            return generateErrorHtml("토큰을 찾을 수 없습니다. 다시 로그인해주세요.", null);
        }

        // Refresh Token에서 이메일과 소셜 제공자 정보 추출
        String email = null;
        String socialProvider = "Unknown";
        
        try {
            if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
                email = jwtTokenProvider.getEmailFromRefreshToken(refreshToken);
                String providerStr = jwtTokenProvider.getProviderFromRefreshToken(refreshToken);

                // provider 정보가 있으면 SocialProvider Enum으로 변환
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

                // DB에서 사용자 정보 조회 (이메일 + 제공자로 유니크하게 조회)
                User user = null;
                if (socialProviderEnum != null) {
                    user = userRepository.findByEmailAndSocialProvider(email, socialProviderEnum).orElse(null);
                } else {
                    // provider 정보가 없는 구형 토큰인 경우, 이메일로만 조회 (하위 호환성)
                    log.warn("토큰에 provider 정보가 없습니다. 구형 토큰일 수 있습니다. 이메일로만 조회합니다.");
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

    /**
     * 현재 로그인한 사용자 정보 조회
     * JWT 인증 (Cookie) 또는 OAuth2 세션 인증 지원
     * 
     * @param oAuth2User OAuth2 인증 사용자 (세션 기반)
     * @param request    HTTP 요청 (Cookie에서 토큰 추출용)
     * @return 사용자 정보
     */
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
        // SecurityContext에서 인증 정보 가져오기
        if (email == null) {
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
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
        
        // 쿠키에서 Access Token 추출
        String accessToken = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        }
        
        // Access Token에서 provider 정보 추출 시도
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
        
        // provider 정보가 없거나 조회 실패한 경우, 이메일로만 조회 (하위 호환성)
        if (user == null) {
            log.warn("provider 정보로 사용자를 찾을 수 없음. 이메일로만 조회합니다: {}", email);
            user = userRepository.findByEmail(email).orElse(null);
        }
        
        if (user == null) {
            log.warn("사용자를 찾을 수 없음: {}", email);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("사용자를 찾을 수 없습니다."));
        }

        log.debug("사용자 정보 조회 성공: {}", email);
        return ResponseEntity.ok(ApiResponse.success(AuthDto.UserInfoResponse.from(user)));
    }

    /**
     * JWT 토큰 검증 및 사용자 정보 반환
     * 
     * @param token Access Token
     * @return 사용자 정보
     */
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<AuthDto.TokenVerifyResponse>> verifyToken(@RequestParam String token) {

        if (!jwtTokenProvider.validateAccessToken(token)) {
            AuthDto.TokenVerifyResponse response = AuthDto.TokenVerifyResponse.fail("유효하지 않은 토큰입니다.");
            return ResponseEntity.status(401).body(ApiResponse.success(response));
        }

        String email = jwtTokenProvider.getEmailFromAccessToken(token);
        
        // provider 정보 추출 및 사용자 조회
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
        
        // provider 정보가 없거나 조회 실패한 경우, 이메일로만 조회 (하위 호환성)
        if (user == null) {
            log.warn("provider 정보로 사용자를 찾을 수 없음. 이메일로만 조회합니다: {}", email);
            user = userRepository.findByEmail(email).orElse(null);
        }

        if (user == null) {
            var response = AuthDto.TokenVerifyResponse.fail("사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(404).body(ApiResponse.success(response));
        }

        var userInfo = AuthDto.UserInfoResponse.from(user);

        var response = AuthDto.TokenVerifyResponse.success(userInfo);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Refresh Token으로 Access Token 갱신
     * 
     * @param request  HTTP 요청
     * @param response HTTP 응답
     * @return 새로운 Access Token과 Refresh Token 정보
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Refresh Token이 없습니다."));
        }
        
        // 2. 토큰 갱신 (기존 Service 로직 사용)
        AuthDto.TokenResponse tokenResponse = refreshTokenService.refreshAccessToken(refreshToken);
        
        // 3. 새 Access Token을 쿠키로 설정
        jakarta.servlet.http.Cookie accessTokenCookie = new jakarta.servlet.http.Cookie("accessToken", tokenResponse.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false); // SSL 설정 전까지 false
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(30 * 60); // 30분
        response.addCookie(accessTokenCookie);
        
        log.info("Access Token 갱신 완료");
        
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    /**
     * 로그아웃 (Cookie 삭제, Refresh Token DB 삭제)
     * 
     * @param request  HTTP 요청
     * @param response HTTP 응답
     * @return 로그인 페이지 HTML
     */
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        
        try {
            // 1. 쿠키에서 Refresh Token 추출
            String refreshToken = null;
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }
            
            // 2. Refresh Token에서 이메일 및 provider 추출 후 DB에서 삭제
            if (refreshToken != null) {
                try {
                    if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
                        String email = jwtTokenProvider.getEmailFromRefreshToken(refreshToken);
                        String providerStr = jwtTokenProvider.getProviderFromRefreshToken(refreshToken);
                        
                        // provider 정보가 있으면 SocialProvider Enum으로 변환하여 삭제
                        if (providerStr != null && !providerStr.isEmpty()) {
                            try {
                                SocialProvider socialProvider = SocialProvider.valueOf(providerStr);
                                refreshTokenService.deleteByUserEmailAndSocialProvider(email, socialProvider);
                                log.info("로그아웃 완료: {} ({})", email, socialProvider);
                            } catch (IllegalArgumentException e) {
                                log.warn("알 수 없는 provider 값: {}. 이메일로만 삭제합니다.", providerStr);
                                refreshTokenService.deleteByUserEmail(email);
                                log.info("로그아웃 완료: {} (provider 정보 없음)", email);
                            }
                        } else {
                            // provider 정보가 없는 구형 토큰인 경우, 이메일로만 삭제 (하위 호환성)
                            log.warn("토큰에 provider 정보가 없습니다. 구형 토큰일 수 있습니다. 이메일로만 삭제합니다.");
                            refreshTokenService.deleteByUserEmail(email);
                            log.info("로그아웃 완료: {} (provider 정보 없음)", email);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Refresh Token 처리 중 오류: {}", e.getMessage());
                }
            }
            
            // 3. Access Token 쿠키 삭제
            jakarta.servlet.http.Cookie accessTokenCookie = new jakarta.servlet.http.Cookie("accessToken", null);
            accessTokenCookie.setMaxAge(0);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setHttpOnly(true);
            response.addCookie(accessTokenCookie);
            
            // 4. Refresh Token 쿠키 삭제
            jakarta.servlet.http.Cookie refreshTokenCookie = new jakarta.servlet.http.Cookie("refreshToken", null);
            refreshTokenCookie.setMaxAge(0);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setHttpOnly(true);
            response.addCookie(refreshTokenCookie);
            
            log.info("쿠키 삭제 완료");
            
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생: {}", e.getMessage(), e);
        }
        
        // 로그인 페이지로 리다이렉트
        return generateLoginHtml();
    }

    /**
     * 로그인 페이지
     * 
     * @return 로그인 HTML
     */
    @GetMapping("/login")
    public String login() {
        return generateLoginHtml();
    }

    /**
     * 에러 페이지
     * 
     * @param message 에러 메시지
     * @param code    에러 코드
     * @return 에러 HTML
     */
    @GetMapping("/error")
    public String error(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String code) {
        if (message == null || message.isEmpty()) {
            message = "알 수 없는 오류가 발생했습니다.";
        }
        return generateErrorHtml(message, code);
    }

    /**
     * 성공 HTML 생성 (Access Token + Refresh Token)
     * 
     * @param accessToken    Access Token
     * @param refreshToken   Refresh Token
     * @param email          사용자 이메일
     * @param socialProvider 소셜 제공자 이름 (Google, Naver, Kakao)
     */
    private String generateSuccessHtml(String accessToken, String refreshToken, String email, String socialProvider) {
        long accessTokenExpiresIn = jwtTokenProvider.getAccessTokenExpiration();
        long refreshTokenExpiresIn = jwtTokenProvider.getRefreshTokenExpiration();

        // 만료 시간을 읽기 쉬운 형식으로 변환
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

    /**
     * 만료 시간을 읽기 쉬운 형식으로 변환
     * 
     * @param milliseconds 밀리초
     * @return 읽기 쉬운 형식 (예: "24시간", "7일")
     */
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

    /**
     * 로그인 HTML 생성
     * 추후 프론트 연결시 삭제 예정 (front_delete)
     * 
     * @return 로그인 페이지 HTML
     */
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
                        .login-buttons {
                            display: flex;
                            flex-direction: column;
                            gap: 15px;
                        }
                        .login-btn {
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            padding: 15px 24px;
                            border: none;
                            border-radius: 8px;
                            font-size: 16px;
                            font-weight: 600;
                            cursor: pointer;
                            transition: all 0.3s;
                            text-decoration: none;
                            color: white;
                        }
                        .login-btn:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
                        }
                        .login-btn:active {
                            transform: translateY(0);
                        }
                        .btn-google {
                            background: #4285f4;
                        }
                        .btn-google:hover {
                            background: #357ae8;
                        }
                        .btn-naver {
                            background: #03c75a;
                        }
                        .btn-naver:hover {
                            background: #02b350;
                        }
                        .btn-kakao {
                            background: #FEE500;
                            color: #000000;
                        }
                        .btn-kakao:hover {
                            background: #FDD835;
                        }
                        .btn-icon {
                            margin-right: 10px;
                            font-size: 20px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🔐 로그인</h1>
                        <p class="subtitle">소셜 계정으로 로그인하세요</p>
                        <div class="login-buttons">
                            <a href="/oauth2/authorization/google" class="login-btn btn-google">
                                <span class="btn-icon">🔵</span>
                                Google로 로그인
                            </a>
                            <a href="/oauth2/authorization/naver" class="login-btn btn-naver">
                                <span class="btn-icon">🟢</span>
                                Naver로 로그인
                            </a>
                            <a href="/oauth2/authorization/kakao" class="login-btn btn-kakao">
                                <span class="btn-icon">🟡</span>
                                Kakao로 로그인
                            </a>
                        </div>
                    </div>
                </body>
                </html>
                """;
    }

    /**
     * 로그아웃 완료 HTML 생성
     * 
     * @param socialProvider 소셜 제공자 이름 (Google, Naver, Kakao)
     */
    private String generateLogoutHtml(String socialProvider) {
        // 소셜 제공자별 로그인 URL
        String loginUrl;
        switch (socialProvider.toLowerCase()) {
            case "google":
                loginUrl = "/oauth2/authorization/google";
                break;
            case "naver":
                loginUrl = "/oauth2/authorization/naver";
                break;
            case "kakao":
                loginUrl = "/oauth2/authorization/kakao";
                break;
            default:
                loginUrl = "/oauth2/authorization/google";
        }

        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
                    <meta http-equiv="Pragma" content="no-cache">
                    <meta http-equiv="Expires" content="0">
                    <title>로그아웃 완료 - MOA2</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 12px;
                            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                            max-width: 500px;
                            width: 90%%;
                            text-align: center;
                        }
                        h1 {
                            color: #333;
                            margin-bottom: 20px;
                        }
                        .logout-message {
                            background: #d1ecf1;
                            color: #0c5460;
                            padding: 12px;
                            border-radius: 8px;
                            margin-bottom: 30px;
                        }
                        a {
                            display: inline-block;
                            padding: 12px 24px;
                            background: #667eea;
                            color: white;
                            text-decoration: none;
                            border-radius: 8px;
                            font-weight: 600;
                            transition: all 0.3s;
                        }
                        a:hover {
                            background: #5568d3;
                            transform: translateY(-2px);
                            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🚪 로그아웃 완료</h1>
                        <div class="logout-message">
                            정상적으로 로그아웃되었습니다.
                        </div>
                        <a href="%s">다시 로그인하기</a>
                    </div>
                </body>
                </html>
                """.formatted(loginUrl);
    }

    /**
     * 에러 HTML 생성
     */
    private String generateErrorHtml(String message, String code) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>오류 - MOA2</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        }
                        .container {
                            background: white;
                            padding: 40px;
                            border-radius: 12px;
                            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                            max-width: 500px;
                            width: 90%%;
                            text-align: center;
                        }
                        h1 {
                            color: #dc3545;
                            margin-bottom: 20px;
                        }
                        .error-message {
                            color: #666;
                            margin-bottom: 30px;
                        }
                        a {
                            display: inline-block;
                            padding: 12px 24px;
                            background: #667eea;
                            color: white;
                            text-decoration: none;
                            border-radius: 8px;
                            font-weight: 600;
                        }
                        a:hover {
                            background: #5568d3;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>❌ 오류 발생</h1>
                        <p class="error-message">%s</p>
                        <a href="/oauth2/authorization/google">다시 로그인하기</a>
                    </div>
                </body>
                </html>
                """.formatted(message);
    }
}
