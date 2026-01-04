package com.moa2.api.auth;

import com.moa2.api.auth.dto.RefreshTokenRequest;
import com.moa2.api.auth.dto.TokenResponse;
import com.moa2.api.auth.dto.UserInfoResponse;
import com.moa2.domain.user.entity.User;
import com.moa2.domain.user.repository.UserRepository;
import com.moa2.global.security.JwtTokenProvider;
import com.moa2.global.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
     * @param session HTTP 세션
     * @param response HTTP 응답 (캐시 방지 헤더 추가용)
     * @return JWT 토큰이 포함된 HTML
     */
    @GetMapping("/success")
    public String success(HttpSession session, HttpServletResponse response) {
        // 캐시 방지 헤더 추가
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        String accessToken = (String) session.getAttribute("access_token");
        String refreshToken = (String) session.getAttribute("refresh_token");
        String email = (String) session.getAttribute("user_email");
        String socialProvider = (String) session.getAttribute("social_provider");

        if (accessToken == null || refreshToken == null || email == null) {
            // 세션 무효화
            session.invalidate();
            return generateErrorHtml("토큰을 찾을 수 없습니다. 다시 로그인해주세요.", null);
        }

        // 세션에서 토큰 제거 (보안상 한 번만 사용)
        session.removeAttribute("access_token");
        session.removeAttribute("refresh_token");
        session.removeAttribute("user_email");
        session.removeAttribute("social_provider");

        // socialProvider가 없으면 기본값 사용
        if (socialProvider == null || socialProvider.trim().isEmpty()) {
            socialProvider = "Google";
        }

        return generateSuccessHtml(accessToken, refreshToken, email, socialProvider);
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     * JWT 인증과 OAuth2 세션 인증 모두 지원
     * @param oAuth2User OAuth2 인증 사용자 (세션 기반)
     * @param request HTTP 요청 (JWT 토큰 추출용)
     * @return 사용자 정보
     */
    @GetMapping("/user")
    public ResponseEntity<UserInfoResponse> getCurrentUser(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            HttpServletRequest request) {
        
        String email = null;
        
        // 1. OAuth2 세션 인증 확인
        if (oAuth2User != null) {
            email = oAuth2User.getAttribute("email");
            log.debug("OAuth2 세션 인증: {}", email);
        }
        
        // 2. JWT 인증 확인 (OAuth2 인증이 없을 경우)
        if (email == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String accessToken = authHeader.substring(7);
                try {
                    if (jwtTokenProvider.validateAccessToken(accessToken)) {
                        email = jwtTokenProvider.getEmailFromAccessToken(accessToken);
                        log.debug("JWT 인증: {}", email);
                    }
                } catch (Exception e) {
                    log.warn("JWT 토큰 검증 실패: {}", e.getMessage());
                }
            }
        }
        
        // 3. 이메일이 없으면 인증 실패
        if (email == null || email.isEmpty()) {
            log.warn("인증되지 않은 요청: /api/auth/user");
            return ResponseEntity.status(401).build();
        }
        
        // 4. 사용자 정보 조회
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("사용자를 찾을 수 없음: {}", email);
            return ResponseEntity.status(404).build();
        }
        
        log.debug("사용자 정보 조회 성공: {}", email);
        return ResponseEntity.ok(UserInfoResponse.from(user));
    }

    /**
     * JWT 토큰 검증 및 사용자 정보 반환
     * @param token Access Token
     * @return 사용자 정보
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestParam String token) {
        Map<String, Object> response = new HashMap<>();

        if (!jwtTokenProvider.validateAccessToken(token)) {
            response.put("valid", false);
            response.put("message", "유효하지 않은 토큰입니다.");
            return ResponseEntity.status(401).body(response);
        }

        String email = jwtTokenProvider.getEmailFromAccessToken(token);
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            response.put("valid", false);
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(404).body(response);
        }

        response.put("valid", true);
        response.put("user", UserInfoResponse.from(user));
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh Token으로 Access Token 갱신
     * @param request Refresh Token 요청
     * @return 새로운 Access Token과 Refresh Token 정보
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = refreshTokenService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 로그아웃 (세션 무효화, Refresh Token 삭제 및 소셜 제공자별 로그아웃)
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param email 요청 파라미터로 전달된 이메일 (HTML 폼)
     * @param accessToken 요청 파라미터로 전달된 Access Token (HTML 폼)
     * @return 소셜 제공자별 로그아웃 URL로 리다이렉트
     */
    @PostMapping("/logout")
    public String logout(
            HttpSession session, 
            HttpServletRequest request,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String accessToken) {
        
        String userEmail = extractEmail(session, request, email, accessToken);
        String socialProvider = "GOOGLE"; // 기본값
        
        // DB에서 사용자 정보 조회하여 소셜 제공자 확인
        if (userEmail != null && !userEmail.isEmpty()) {
            try {
                refreshTokenService.deleteByUserEmail(userEmail);
                log.info("Refresh Token 삭제 완료: {}", userEmail);
                
                // 소셜 제공자 확인
                User user = userRepository.findByEmail(userEmail).orElse(null);
                if (user != null) {
                    socialProvider = user.getSocialProvider().name();
                }
            } catch (Exception e) {
                log.error("Refresh Token 삭제 실패: {}", e.getMessage());
            }
        }
        
        session.invalidate();
        log.info("사용자 로그아웃: {} ({})", userEmail, socialProvider);
        
        // 소셜 제공자별 로그아웃 URL
        String logoutUrl;
        switch (socialProvider) {
            case "GOOGLE":
                logoutUrl = "https://accounts.google.com/logout?continue=http://localhost:8081/api/auth/logout/complete?provider=google";
                break;
            case "NAVER":
                logoutUrl = "https://nid.naver.com/nidlogin.logout?returl=http://localhost:8081/api/auth/logout/complete?provider=naver";
                break;
            case "KAKAO":
                logoutUrl = "https://kauth.kakao.com/oauth/logout?client_id=YOUR_KAKAO_CLIENT_ID&logout_redirect_uri=http://localhost:8081/api/auth/logout/complete?provider=kakao";
                break;
            default:
                logoutUrl = "http://localhost:8081/api/auth/logout/complete?provider=unknown";
        }
        
        return "redirect:" + logoutUrl;
    }
    
    /**
     * 이메일 추출 (요청 파라미터 → 세션 → Access Token → Refresh Token 순서)
     */
    private String extractEmail(HttpSession session, HttpServletRequest request, 
                               String emailParam, String accessTokenParam) {
        // 1. 요청 파라미터에서 이메일
        if (emailParam != null && !emailParam.isEmpty()) {
            return emailParam;
        }
        
        // 2. 세션에서 이메일
        String email = (String) session.getAttribute("user_email");
        if (email != null && !email.isEmpty()) {
            return email;
        }
        
        // 3. Access Token에서 이메일 추출
        String token = accessTokenParam;
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }
        
        if (token != null && !token.isEmpty()) {
            try {
                if (jwtTokenProvider.validateAccessToken(token)) {
                    return jwtTokenProvider.getEmailFromAccessToken(token);
                }
            } catch (Exception e) {
                log.debug("Access Token에서 이메일 추출 실패: {}", e.getMessage());
            }
        }
        
        // 4. Refresh Token에서 이메일 추출
        String refreshToken = (String) session.getAttribute("refresh_token");
        if (refreshToken != null) {
            try {
                if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
                    return jwtTokenProvider.getEmailFromRefreshToken(refreshToken);
                }
            } catch (Exception e) {
                log.debug("Refresh Token에서 이메일 추출 실패: {}", e.getMessage());
            }
        }
        
        return null;
    }


    /**
     * 로그아웃 완료 페이지
     * @param provider 소셜 제공자 (google, naver, kakao)
     * @return 로그아웃 완료 HTML
     */
    @GetMapping("/logout/complete")
    public String logoutComplete(@RequestParam(required = false) String provider) {
        // 캐시 방지
        String socialProviderName = "Google"; // 기본값
        if (provider != null) {
            switch (provider.toLowerCase()) {
                case "google" -> socialProviderName = "Google";
                case "naver" -> socialProviderName = "Naver";
                case "kakao" -> socialProviderName = "Kakao";
            }
        }
        return generateLogoutHtml(socialProviderName);
    }

    /**
     * 에러 페이지
     * @param message 에러 메시지
     * @param code 에러 코드
     * @return 에러 HTML
     */
    @GetMapping("/error")
    public String error(@RequestParam(required = false) String message,
                       @RequestParam(required = false) String code) {
        if (message == null || message.isEmpty()) {
            message = "알 수 없는 오류가 발생했습니다.";
        }
        return generateErrorHtml(message, code);
    }

    /**
     * 성공 HTML 생성 (Access Token + Refresh Token)
     * @param accessToken Access Token
     * @param refreshToken Refresh Token
     * @param email 사용자 이메일
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
                    
                    <div class="button-group" style="margin-top: 20px;">
                        <button class="logout-btn" onclick="logout()">🚪 로그아웃</button>
                    </div>
                    
                    <div class="info-section">
                        <div class="info-item">
                            <span class="info-label">이메일:</span> %s
                        </div>
                        <div class="info-item">
                            <span class="info-label">사용 방법:</span> API 요청 시 Authorization 헤더에 "Bearer {Access Token}" 형식으로 전송하세요.
                        </div>
                        <div class="info-item">
                            <span class="info-label">토큰 갱신:</span> Access Token이 만료되면 POST /api/auth/refresh 엔드포인트에 Refresh Token을 전송하여 새 Access Token을 받으세요.
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
                    
                    async function refreshAccessToken() {
                        const refreshToken = document.getElementById('refresh-token').value.trim();
                        
                        if (!refreshToken) {
                            alert('Refresh Token이 없습니다.');
                            return;
                        }
                        
                        try {
                            const response = await fetch('/api/auth/refresh', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify({ refreshToken: refreshToken })
                            });
                            
                            if (response.ok) {
                                const data = await response.json();
                                document.getElementById('access-token').value = data.accessToken;
                                alert('✅ Access Token이 갱신되었습니다!');
                            } else {
                                const error = await response.json();
                                alert('❌ 토큰 갱신 실패: ' + (error.message || error.error || '알 수 없는 오류'));
                            }
                        } catch (err) {
                            alert('토큰 갱신 중 오류 발생: ' + err.message);
                        }
                    }
                    
                    function logout() {
                        if (confirm('로그아웃하시겠습니까?')) {
                            const form = document.createElement('form');
                            form.method = 'POST';
                            form.action = '/api/auth/logout';
                            
                            // 이메일을 파라미터로 전달
                            const emailInput = document.createElement('input');
                            emailInput.type = 'hidden';
                            emailInput.name = 'email';
                            emailInput.value = userEmail;
                            form.appendChild(emailInput);
                            
                            // Access Token도 전달 (백업용)
                            const accessToken = document.getElementById('access-token').value.trim();
                            const tokenInput = document.createElement('input');
                            tokenInput.type = 'hidden';
                            tokenInput.name = 'accessToken';
                            tokenInput.value = accessToken;
                            form.appendChild(tokenInput);
                            
                            document.body.appendChild(form);
                            form.submit();
                        }
                    }
                </script>
            </body>
            </html>
            """.formatted(
                socialProvider,  // 소셜 제공자 이름
                accessTokenExpiresInText, accessToken, 
                refreshTokenExpiresInText, refreshToken, 
                email,  // HTML 표시용
                email   // JavaScript 변수용
            );
    }

    /**
     * 만료 시간을 읽기 쉬운 형식으로 변환
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
     * 로그아웃 완료 HTML 생성
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

