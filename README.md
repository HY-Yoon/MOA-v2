# Moa Place v2

---

## 프로젝트 소개
- 공연 정보 및 좌석 등록/관리하고, 사용자 예매 기능을 제공하는 웹 인터페이스입니다.
- [모아 플레이스 v1](https://github.com/psmin77/MOAPLACE-page)의 주요 기능을 위주로 Next.js와 React, Java, Redis 등 기술 스택을 업그레이드하여 구현합니다.  

### 프로젝트 메뉴
- **관리자**
    - 공연 조회 및 관리
    - 좌석도 조회 및 관리
    - 회원 목록 조회 및 관리
- **사용자**
    - 로그인 및 회원가입
    - 공연 정보 조회
    - 공연 예매 및 결제
    - 회원 정보 및 예매내역 조회 관리 등


## 주요 기능
- Shadcn 디자인 시스템 컴포넌트를 사용한 공통 컴포넌트 UI/UX 구현
- Canvas 활용한 공연장 좌석도 및 예매 좌석 구현
- Redis를 통해 실시간 좌석 선점 및 예매 시스템 관리
- 소셜 API를 통한 로그인 및 회원가입, 본인 인증 등

## 기술 스택
### **공통**
- Monorepo

### **Frontend** 
- Next.js 16.1.1, React 19.2.3
- TypeScript 5
- Tailwind, Shadcn
- Tanstack Query React 5
- Pnpm

### **Backend**
- Java 17
- Redis

## 개발 가이드 라인
### 코드 컨벤션
- **명명 규칙**:
    - 파일/컴포넌트명: PascalCase
    - 유틸리티/훅: camelCase
    - 함수/변수: camelCase
    - 상수: UPPER_SNAKE_CASE

### Git 워크플로우
- **브랜치 구조**
  - `main`: 최종 메인 브랜치
  - `develop`: 개발 메인 브랜치 (단위 테스트 완료 후 머지)
  - `feature/*`: 신규 기능 개발
  - `fix/*`: 버그 수정
- **커밋 컨벤션**
  - 일반적인 커밋 컨벤션 준수
  - `[ALL]`, `[FE]`, `[BE]`: 공통, 프론트엔드, 백엔드 태그 추가
  - `feat`, `fix`, `refactor`, `chore`, `docs`, `style` 등

