# 🎭 Moa Place v2 🎭

공연 정보 관리와 예매, 결제 기능을 사용자/관리자 메뉴로 제공하는 웹 서비스입니다.  
[모아 플레이스 v1](https://github.com/psmin77/MOAPLACE-page)의 주요 기능을 기반으로 기술 스택을 업그레이드하여 재구성했습니다.

## 🎯 프로젝트 개요

### 사용자 기능
- 로그인/회원가입
- 공연 목록 및 상세 조회
- 좌석 선택, 예매, 결제
- 마이페이지 예매 내역 조회/관리

### 관리자 기능
- 공연 등록/수정/조회/삭제
- 좌석도 등록/수정/조회/삭제
- 회원 목록 조회/관리

## ✅ 주요 기능
- Shadcn 기반 공통 UI 컴포넌트 설계 및 재사용
- Canvas 기반 좌석도 렌더링/선택 인터랙션
- Redis 기반 실시간 좌석 선점 및 예매 동시성 제어
- 소셜 로그인 및 인증 흐름 지원

## 🗂️ 모노레포 구조

```text
MOA-v2/
├── @frontend/   # Next.js 프론트엔드
├── @backend/    # Spring Boot 백엔드
├── @shared/     # 공통 타입 파일
└── README.md    # 메인
```

## 🛠️ 기술 스택

### Frontend
- Next.js 16.1.1, React 19.2.3, TypeScript 5
- Tailwind CSS, Shadcn/ui
- TanStack Query v5, React Hook Form, Zod
- Pnpm

### Backend
- Java 17, Spring Boot 3
- Spring Data JPA, Spring Security, OAuth2 Client
- Redis, PostgreSQL, Kafka
- Gradle (Kotlin DSL)

## ⚙️ 프로젝트 설정

### 1) 사전 요구사항
- Node.js 20+
- pnpm 8+
- Java 17
- PostgreSQL, Redis

### 2) 백엔드 실행
- `@backend/src/main/resources/application.properties.example`를 기준으로 로컬 설정 파일을 구성합니다.
- DB, Redis, OAuth, JWT 관련 환경변수를 주입해 실행합니다.
- 기본 포트: `8081`

```bash
cd @backend
cp src/main/resources/application.properties.example src/main/resources/application-local.properties
./gradlew bootRun
```


### 3) 프론트엔드 실행
- 환경 설정: `@frontend/.env.local`
```env
NEXT_PUBLIC_BACKEND_URL=http://moa.hee-factory.com
```

- 기본 포트: `3000`

```bash
cd @frontend
pnpm install
pnpm dev
```

## 👉 개발 가이드라인

### 코드 컨벤션
- 파일/컴포넌트: `PascalCase`
- 유틸리티/훅: `camelCase`
- 함수/변수: `camelCase`
- 상수: `UPPER_SNAKE_CASE`

### Git 워크플로우
- 브랜치
  - `main`: 최종 배포
  - `develop`: 통합 개발
  - `feature/*`: 기능 개발
  - `fix/*`: 버그 수정
- 커밋
  - Conventional Commits 권장 (`feat`, `fix`, `refactor`, `chore`, `docs`, `style`)
  - 범위 태그 사용: `[ALL]`, `[FE]`, `[BE]`

## 📚 기타
- 프론트엔드 상세 안내: `@frontend/README.md`

