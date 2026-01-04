# Google 소셜 로그인 + JWT 인증 구현 가이드

## 1. 전체 시나리오 (Flow)

### 현재 구현된 시나리오 (백엔드 테스트용)

1. 사용자가 `/oauth2/authorization/google`로 접근하여 구글 로그인
2. Google 로그인 페이지로 리다이렉트
3. 사용자가 Google 계정 선택 및 로그인
4. Google에서 `/login/oauth2/code/google`로 콜백
5. 인증 성공 시 `CustomOAuth2UserService`에서 DB 저장/업데이트
6. `OAuth2SuccessHandler`에서 Access Token과 Refresh Token 생성
7. Refresh Token을 DB에 저장
8. 핸들러가 `/api/auth/success`로 리다이렉트 (세션을 이용해 토큰 전달)
9. 브라우저 화면에 **Access Token, Refresh Token과 복사 버튼**이 포함된 간단한 HTML 표시

### 프론트엔드 연결 시나리오 (추후 구현)

1. 프론트엔드(localhost:3000) → "구글 로그인" 버튼 클릭
2. 백엔드(localhost:8081/oauth2/authorization/google)로 요청
3. Google 로그인 페이지
4. 로그인 성공
5. 백엔드(localhost:8081/login/oauth2/code/google)로 콜백
6. 백엔드에서 JWT 생성
7. 프론트엔드(localhost:3000/oauth/redirect?token=xxx)로 리다이렉트

---

## 2. 핵심 구현 시나리오 (Flow)

1. 사용자가 `/oauth2/authorization/google`로 접근하여 구글 로그인.
2. 인증 성공 시 `CustomOAuth2UserService`에서 DB 저장/업데이트.
3. `OAuth2SuccessHandler`에서 Access Token과 Refresh Token 생성.
4. Refresh Token을 DB에 저장.
5. 핸들러가 `/api/auth/success`로 리다이렉트 (세션 등을 이용해 토큰 전달).
6. 브라우저 화면에 **Access Token, Refresh Token과 복사 버튼**이 포함된 간단한 HTML 표시.

---

## 3. 구현된 API 엔드포인트

### 3.1 인증 관련 API

#### `GET /api/auth/success`
- **설명**: OAuth2 로그인 성공 후 Access Token과 Refresh Token을 표시하는 HTML 페이지
- **인증**: 불필요 (공개 경로)
- **요청**: 세션에 `access_token`, `refresh_token`, `user_email`이 있어야 함
- **응답**: HTML 페이지 (Access Token, Refresh Token, 복사 버튼, 갱신 테스트 버튼, 로그아웃 버튼 포함)
- **용도**: 백엔드 테스트용 (프론트엔드 연결 시 변경 필요)

#### `GET /api/auth/user`
- **설명**: 현재 로그인한 사용자 정보 조회
- **인증**: 필요 (세션 기반 또는 JWT)
- **요청 헤더**: 
  - 세션 기반: 세션 쿠키
  - JWT 기반: `Authorization: Bearer {token}`
- **응답**:
  ```json
  {
    "email": "user@example.com",
    "name": "사용자 이름",
    "picture": "https://lh3.googleusercontent.com/...",
    "provider": "GOOGLE",
    "role": "USER"
  }
  ```
- **에러**:
  - `401 Unauthorized`: 인증되지 않음
  - `404 Not Found`: 사용자를 찾을 수 없음

#### `POST /api/auth/refresh`
- **설명**: Refresh Token으로 Access Token 갱신
- **인증**: 불필요 (공개 경로)
- **요청 Body**:
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
  ```
- **응답 (성공)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "accessTokenExpiresIn": 86400000,
    "refreshTokenExpiresIn": 604800000,
    "email": "user@example.com"
  }
  ```
- **에러**:
  - `401 Unauthorized`: Refresh Token이 만료되었거나 유효하지 않음
  - `404 Not Found`: Refresh Token을 찾을 수 없음

#### `GET /api/auth/verify?token={access_token}`
- **설명**: Access Token 검증 및 사용자 정보 반환
- **인증**: 불필요 (공개 경로)
- **요청 파라미터**: `token` (Access Token)
- **응답 (성공)**:
  ```json
  {
    "valid": true,
    "user": {
      "email": "user@example.com",
      "name": "사용자 이름",
      "picture": "https://lh3.googleusercontent.com/...",
      "provider": "GOOGLE",
      "role": "USER"
    }
  }
  ```
- **응답 (실패)**:
  ```json
  {
    "valid": false,
    "message": "유효하지 않은 토큰입니다."
  }
  ```
- **에러**:
  - `401 Unauthorized`: 토큰이 유효하지 않음
  - `404 Not Found`: 사용자를 찾을 수 없음

#### `POST /api/auth/logout`
- **설명**: 로그아웃 (세션 무효화, Refresh Token 삭제 및 Google 로그아웃)
- **인증**: 불필요 (공개 경로)
- **요청**: 세션 쿠키
- **응답**: Google 로그아웃 URL로 리다이렉트 후 완료 페이지
- **동작**:
  1. DB에서 Refresh Token 삭제
  2. 애플리케이션 세션 무효화
  3. Google 로그아웃 URL로 리다이렉트
  4. Google 세션 종료
  5. `/api/auth/logout/complete`로 리다이렉트

#### `GET /api/auth/logout`
- **설명**: 로그아웃 (GET 방식 지원)
- **인증**: 불필요 (공개 경로)
- **동작**: `POST /api/auth/logout`과 동일 (Refresh Token 삭제 포함)

#### `GET /api/auth/logout/complete`
- **설명**: 로그아웃 완료 페이지
- **인증**: 불필요 (공개 경로)
- **응답**: 로그아웃 완료 HTML 페이지

### 3.2 OAuth2 인증 엔드포인트

#### `GET /oauth2/authorization/google`
- **설명**: Google OAuth2 로그인 시작
- **인증**: 불필요 (공개 경로)
- **동작**: Google 로그인 페이지로 리다이렉트
- **특징**: `prompt=select_account` 파라미터가 자동 추가되어 항상 계정 선택 화면 표시

#### `GET /login/oauth2/code/google`
- **설명**: Google OAuth2 콜백 엔드포인트
- **인증**: 불필요 (Google에서 자동 호출)
- **동작**: 
  1. Google에서 인증 코드 전달
  2. Access Token 교환
  3. 사용자 정보 로드
  4. DB 저장/업데이트
  5. JWT 생성
  6. `/api/auth/success`로 리다이렉트

---

## 4. 데이터베이스 테이블 확인

### 4.1 `users` 테이블

#### 주요 컬럼
- `id`: 사용자 ID (PK)
- `email`: 이메일 주소 (Google 계정 이메일)
- `name`: 사용자 이름
- `picture`: 프로필 이미지 URL (Google 프로필 이미지)
- `social_provider`: 소셜 제공자 (`GOOGLE`, `KAKAO`, `NAVER`)
- `provider_id`: 제공자 고유 ID (Google의 경우 `sub` 값)
- `role`: 사용자 역할 (`USER`, `ADMIN`)
- `status`: 사용자 상태 (`ACTIVE`, `DELETED`, `SUSPENDED`)
- `is_verified`: 본인인증 여부
- `created_at`: 생성 시간
- `updated_at`: 수정 시간

#### 확인 쿼리

```sql
-- 최근 로그인한 Google 사용자 확인
SELECT 
    id,
    email,
    name,
    picture,
    social_provider,
    provider_id,
    role,
    status,
    is_verified,
    created_at,
    updated_at
FROM users
WHERE social_provider = 'GOOGLE'
ORDER BY created_at DESC
LIMIT 10;

-- 특정 사용자 확인
SELECT * FROM users WHERE email = 'user@example.com';

-- 신규 사용자 등록 확인
SELECT * FROM users 
WHERE social_provider = 'GOOGLE' 
  AND created_at >= NOW() - INTERVAL '1 hour'
ORDER BY created_at DESC;

-- 기존 사용자 정보 업데이트 확인
SELECT 
    email,
    name,
    picture,
    created_at,
    updated_at,
    updated_at - created_at as time_diff
FROM users
WHERE social_provider = 'GOOGLE'
  AND updated_at > created_at
ORDER BY updated_at DESC;
```

#### 유니크 제약조건
- `(email, social_provider)`: 동일한 이메일이 다른 제공자로 등록 가능
- `provider_id`: 제공자별 고유 ID

---

## 5. 추후 프론트엔드 연결 방법

### 5.1 현재 구조의 문제점

현재 구현은 **백엔드 테스트용**으로 설계되어 있어 프론트엔드 연결 시 수정이 필요합니다:

1. **세션 기반 토큰 전달**: 현재는 세션을 통해 토큰을 전달하지만, 프론트엔드는 다른 도메인(포트)이므로 세션 공유 불가
2. **HTML 응답**: `/api/auth/success`가 HTML을 반환하지만, 프론트엔드는 JSON 또는 리다이렉트가 필요

### 5.2 프론트엔드 연결 시나리오

#### 옵션 1: URL 파라미터로 토큰 전달 (권장 - Refresh Token 포함)

**수정 필요 파일**: `OAuth2SuccessHandler.java`

```java
// 현재 코드
getRedirectStrategy().sendRedirect(request, response, "/api/auth/success");

// 프론트엔드 연결 시 수정 (Access Token + Refresh Token 전달)
String frontendUrl = "http://localhost:3000/oauth/redirect";
String redirectUrl = frontendUrl 
    + "?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
    + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
getRedirectStrategy().sendRedirect(request, response, redirectUrl);
```

**프론트엔드 처리**:
```typescript
// /oauth/redirect 페이지
const searchParams = new URLSearchParams(window.location.search);
const accessToken = searchParams.get('accessToken');
const refreshToken = searchParams.get('refreshToken');

if (accessToken && refreshToken) {
  // localStorage에 두 토큰 모두 저장
  localStorage.setItem('access_token', accessToken);
  localStorage.setItem('refresh_token', refreshToken);
  
  // 메인 페이지로 리다이렉트
  window.location.href = '/';
}
```

#### 옵션 2: JSON API로 토큰 반환 (Refresh Token 포함)

**새로운 엔드포인트 추가**: `AuthController.java`

```java
@GetMapping("/token")
public ResponseEntity<TokenResponse> getToken(HttpSession session) {
    String accessToken = (String) session.getAttribute("access_token");
    String refreshToken = (String) session.getAttribute("refresh_token");
    String email = (String) session.getAttribute("user_email");
    
    if (accessToken == null || refreshToken == null || email == null) {
        return ResponseEntity.status(401).build();
    }
    
    // 세션에서 토큰 제거
    session.removeAttribute("access_token");
    session.removeAttribute("refresh_token");
    session.removeAttribute("user_email");
    
    TokenResponse response = TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpiration())
            .refreshTokenExpiresIn(jwtTokenProvider.getRefreshTokenExpiration())
            .email(email)
            .build();
    
    return ResponseEntity.ok(response);
}
```

**OAuth2SuccessHandler 수정**:
```java
// 프론트엔드로 리다이렉트
String frontendUrl = "http://localhost:3000/oauth/callback";
getRedirectStrategy().sendRedirect(request, response, frontendUrl);
```

**프론트엔드 처리**:
```typescript
// /oauth/callback 페이지
useEffect(() => {
  // 백엔드에서 토큰 가져오기
  fetch('http://localhost:8081/api/auth/token', {
    credentials: 'include' // 세션 쿠키 포함
  })
  .then(res => res.json())
  .then(data => {
    localStorage.setItem('access_token', data.accessToken);
    localStorage.setItem('refresh_token', data.refreshToken);
    window.location.href = '/';
  });
}, []);
```

### 5.3 프론트엔드에서 JWT 사용 방법 (Refresh Token 포함)

#### API 요청 시 Access Token 포함 및 자동 갱신

```typescript
// Axios 예시
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8081',
});

// 요청 인터셉터: Access Token 자동 추가
apiClient.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem('access_token');
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// 응답 인터셉터: 401 에러 시 Refresh Token으로 갱신
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 401 에러이고, 아직 재시도하지 않은 경우
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Refresh Token으로 Access Token 갱신
        const refreshToken = localStorage.getItem('refresh_token');
        if (!refreshToken) {
          throw new Error('Refresh Token이 없습니다.');
        }

        const response = await axios.post('http://localhost:8081/api/auth/refresh', {
          refreshToken: refreshToken
        });

        const { accessToken, refreshToken: newRefreshToken } = response.data;

        // 새 토큰 저장
        localStorage.setItem('access_token', accessToken);
        if (newRefreshToken) {
          localStorage.setItem('refresh_token', newRefreshToken);
        }

        // 원래 요청 재시도
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);

      } catch (refreshError) {
        // Refresh Token 갱신 실패 시 로그인 페이지로 리다이렉트
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);
```

#### 사용자 정보 조회

```typescript
// 현재 사용자 정보 가져오기
const getCurrentUser = async () => {
  try {
    const response = await apiClient.get('/api/auth/user');
    return response.data;
  } catch (error) {
    console.error('사용자 정보 조회 실패:', error);
    return null;
  }
};
```

#### Access Token 검증

```typescript
// Access Token 유효성 검증
const verifyAccessToken = async (token: string) => {
  try {
    const response = await apiClient.get(`/api/auth/verify?token=${token}`);
    return response.data;
  } catch (error) {
    return { valid: false };
  }
};

// 현재 저장된 Access Token 검증
const checkTokenValidity = async () => {
  const accessToken = localStorage.getItem('access_token');
  if (!accessToken) {
    return false;
  }

  const result = await verifyAccessToken(accessToken);
  if (!result.valid) {
    // Access Token이 만료되었으면 Refresh Token으로 갱신 시도
    const newToken = await refreshAccessToken();
    return newToken !== null;
  }

  return true;
};
```

### 5.4 로그아웃 처리

```typescript
// 로그아웃 함수
const logout = async () => {
  try {
    await apiClient.post('/api/auth/logout');
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    window.location.href = '/login';
  } catch (error) {
    console.error('로그아웃 실패:', error);
    // 에러가 나도 토큰은 삭제
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    window.location.href = '/login';
  }
};
```

### 5.5 CORS 설정 (필요한 경우)

**SecurityConfig.java에 추가**:
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}

// SecurityFilterChain에 추가
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

### 5.6 환경 변수 설정

**프론트엔드 `.env`**:
```env
VITE_API_URL=http://localhost:8081
VITE_OAUTH_REDIRECT_URL=http://localhost:3000/oauth/redirect
```

**백엔드 `application.properties`** (프론트엔드 연결 시):
```properties
# 프론트엔드 URL
frontend.url=http://localhost:3000
frontend.oauth.redirect.path=/oauth/redirect
```

---

## 6. 구현된 주요 클래스

### 6.1 서비스 클래스
- `CustomOAuth2UserService`: OAuth2 사용자 정보 로드 및 DB 저장/업데이트
- `JwtTokenProvider`: JWT 토큰 생성, 검증, 파싱

### 6.2 핸들러 클래스
- `OAuth2SuccessHandler`: OAuth2 로그인 성공 시 JWT 생성 및 리다이렉트
- `JwtAuthenticationFilter`: JWT 토큰 인증 필터

### 6.3 설정 클래스
- `SecurityConfig`: Spring Security 설정 (OAuth2, JWT 필터)
- `CustomOAuth2AuthorizationRequestResolver`: OAuth2 인증 요청에 `prompt=select_account` 추가

### 6.4 DTO 클래스
- `OAuthAttributes`: OAuth2 응답 파싱 및 User 엔티티 변환
- `UserInfoResponse`: 사용자 정보 응답 DTO

---

## 7. 테스트 방법

### 7.1 백엔드 테스트

1. **Google 로그인**: `http://localhost:8081/oauth2/authorization/google`
2. **JWT 확인**: 자동으로 `/api/auth/success`로 리다이렉트
3. **토큰 검증**: `curl "http://localhost:8081/api/auth/verify?token=YOUR_TOKEN"`
4. **사용자 정보 조회**: `curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8081/api/auth/user`
5. **로그아웃**: `/api/auth/success` 페이지에서 로그아웃 버튼 클릭

### 7.2 DB 확인

```sql
-- 최근 로그인한 사용자 확인
SELECT * FROM users WHERE social_provider = 'GOOGLE' ORDER BY updated_at DESC LIMIT 5;
```

---

## 8. 주의사항

1. **세션 기반 토큰 전달**: 현재는 세션을 사용하지만, 프론트엔드 연결 시 URL 파라미터나 JSON API로 변경 필요
2. **CORS 설정**: 프론트엔드와 백엔드가 다른 포트인 경우 CORS 설정 필요
3. **환경 변수**: `.env` 파일의 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` 확인
4. **Redirect URI**: Google Cloud Console의 Redirect URI가 정확히 설정되어 있어야 함
5. **JWT 만료 시간**: `JWT_EXPIRATION` 환경 변수 확인 (밀리초 단위)

---

## 9. 참고 자료

- Spring Security OAuth2: https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html
- Google OAuth2: https://developers.google.com/identity/protocols/oauth2
- JWT: https://jwt.io/

---
##.Refresh_token

[refresh_token]
1. 로그인 성공
→ Access Token (24시간) + Refresh Token (7일) 발급
→ Refresh Token을 DB에 저장

2. API 요청
   → Access Token이 유효하면 정상 처리
   → Access Token이 만료되면 401 반환

3. 토큰 갱신 요청 (POST /api/auth/refresh)
   → Refresh Token 전달
   → DB에서 검증
   → 새로운 Access Token 발급
   → Refresh Token 갱신 (optional)

4. 로그아웃
   → DB에서 Refresh Token 삭제MOA-v2 프로젝트에 Refresh Token 기능을 DB 기반으로 구현해줘.

## 현재 상황
- Google OAuth2 로그인 구현 완료
- JWT Access Token 발급 완료 (24시간)
- Refresh Token 없음

## 목표
- DB에 Refresh Token 저장
- Access Token 만료 시 갱신 가능
- 나중에 Redis로 쉽게 마이그레이션 가능하도록 설계

## 구현 필요 사항

### 1. RefreshToken 엔티티 생성
파일: domain/auth/RefreshToken.java (새 패키지)

필드:
- id (Long, @GeneratedValue)
- token (String, unique, 길이 500)
- userEmail (String, 인덱스)
- expiryDate (LocalDateTime)
- createdAt (LocalDateTime)

메서드:
- isExpired(): boolean - 만료 체크

### 2. RefreshTokenRepository 생성
파일: domain/auth/RefreshTokenRepository.java

메서드:
- Optional<RefreshToken> findByToken(String token)
- Optional<RefreshToken> findByUserEmail(String userEmail)
- void deleteByUserEmail(String userEmail)
- void deleteByToken(String token)

### 3. JwtTokenProvider 수정
파일: global/security/JwtTokenProvider.java

메서드 추가:
- String createRefreshToken(String email): Refresh Token 생성 (7일)
- boolean validateRefreshToken(String token): Refresh Token 검증
- String getEmailFromRefreshToken(String token): 이메일 추출

기존 createToken 메서드는 Access Token 전용으로 유지

### 4. TokenResponse DTO 수정
파일: api/auth/dto/TokenResponse.java

필드 추가:
- accessToken (기존 token → 이름 변경)
- refreshToken (새로 추가)
- accessTokenExpiresIn (Long, ms 단위)
- refreshTokenExpiresIn (Long, ms 단위)

### 5. RefreshTokenService 생성
파일: global/service/RefreshTokenService.java

메서드:
- RefreshToken createRefreshToken(String email): DB에 저장
- void verifyExpiration(RefreshToken token): 만료 체크 후 예외 처리
- RefreshToken findByToken(String token): 조회
- void deleteByUserEmail(String email): 삭제 (로그아웃 시)
- TokenResponse refreshAccessToken(String refreshToken): 새 Access Token 발급

### 6. OAuth2SuccessHandler 수정
파일: global/handler/OAuth2SuccessHandler.java

변경 사항:
- Access Token + Refresh Token 둘 다 생성
- Refresh Token은 DB에 저장
- 세션에 둘 다 저장

### 7. AuthController 수정
파일: api/auth/AuthController.java

엔드포인트 추가:
- POST /api/auth/refresh
    - Request Body: { "refreshToken": "..." }
    - Response: TokenResponse (새 Access Token + 기존 Refresh Token)
    - Refresh Token 검증 → 새 Access Token 발급

- POST /api/auth/logout 수정
    - Refresh Token도 DB에서 삭제

GET /api/auth/success 수정:
- Access Token과 Refresh Token 둘 다 표시
- 각 토큰의 만료 시간 표시

### 8. 예외 처리
파일: global/exception/RefreshTokenException.java (새로 생성)

- RefreshTokenNotFoundException
- RefreshTokenExpiredException

global/exception/GlobalExceptionHandler.java에 핸들러 추가

### 9. application.properties 수정
환경변수 추가:
```properties
# JWT - Access Token
jwt.access.secret=${JWT_SECRET}
jwt.access.expiration=${JWT_EXPIRATION:86400000}

# JWT - Refresh Token
jwt.refresh.secret=${JWT_REFRESH_SECRET}
jwt.refresh.expiration=${JWT_REFRESH_EXPIRATION:604800000}
```

### 10. .env 파일 업데이트
추가:
```env
JWT_REFRESH_SECRET=moa-v2-refresh-token-secret-key-different-from-access-token
JWT_REFRESH_EXPIRATION=604800000
```

## 요구사항

### 설계 원칙
1. **DB 스키마 단순화**: Redis 마이그레이션 대비
2. **토큰 보안**: Refresh Token은 Access Token과 다른 시크릿 키 사용
3. **자동 만료**: DB 레코드는 주기적으로 정리 (스케줄러는 나중에)
4. **One Refresh Token Per User**: 사용자당 하나의 Refresh Token만 유지

### 코드 스타일
- 기존 MOA-v2 구조 유지
- Lombok 사용
- 예외 처리 global/exception 활용
- 트랜잭션 처리 (@Transactional)

### 테스트 시나리오
1. 로그인 → Access Token + Refresh Token 받기
2. Access Token으로 /api/auth/user 호출 (성공)
3. 24시간 후 (테스트는 임의로 만료시킴)
4. POST /api/auth/refresh로 새 Access Token 받기
5. 새 Access Token으로 /api/auth/user 호출 (성공)
6. 로그아웃 → DB에서 Refresh Token 삭제 확인

### HTML 응답 개선
/api/auth/success 페이지에:
- Access Token 표시
- Refresh Token 표시 (별도)
- 각 토큰 만료 시간 표시
- "Refresh Token으로 갱신" 테스트 버튼 추가

## 추가 고려사항
- Refresh Token은 HttpOnly Cookie로도 전달 가능 (선택사항)
- Refresh Token Rotation (사용 시 새로 발급) 구현 가능
- 나중에 Redis 전환 시 RefreshTokenRepository만 수정하면 됨
