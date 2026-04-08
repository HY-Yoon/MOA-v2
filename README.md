# Moa Place v2

> 공연 예매 서비스 — 대용량 트래픽 처리를 목표로 Redis 대기열·분산 락, Kafka 비동기 처리, Terraform AWS 인프라 자동화를 적용한 풀스택 프로젝트

---

## 🗂️ 모노레포 구조

```
MOA-v2/
├── @frontend/   # Next.js 프론트엔드
├── @backend/    # Spring Boot 백엔드
├── @shared/     # 공통 타입 파일
├── terraform/   # AWS 인프라 (IaC)
├── k6/          # 부하 테스트 시나리오
└── README.md
```

---

## 🛠️ 기술 스택

### 공통

- Monorepo (pnpm workspace)

### Frontend

| 분류 | 기술 |
|------|------|
| Framework | Next.js 16.1.1, React 19.2.3 |
| Language | TypeScript 5 |
| Styling | Tailwind CSS, Shadcn/ui |
| 데이터 페칭 | TanStack Query v5 |
| Form | React Hook Form, Zod |
| 패키지 매니저 | pnpm |

### Backend

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.1 |
| ORM | Spring Data JPA (Hibernate) |
| DB | PostgreSQL |
| Cache / 대기열 | Redis (Spring Data Redis) |
| 분산 락 | Redisson 3.24.3 |
| 스케줄 락 | ShedLock 5.9.1 |
| 메시징 | Apache Kafka (spring-kafka) |
| 보안 | Spring Security, OAuth2, JWT |
| 스토리지 | AWS S3 (SDK v2) |
| API 문서 | SpringDoc OpenAPI 2.7.0 |
| 빌드 | Gradle Kotlin DSL |

### 인프라 · DevOps

| 분류 | 기술 |
|------|------|
| IaC | Terraform |
| Cloud | AWS (VPC, EC2, ASG, S3, CloudFront, Route53, EventBridge) |
| 컨테이너 | Docker, Docker Compose |
| 부하 테스트 | k6 |
| 모니터링 | Prometheus, Grafana, Spring Actuator |

---

## ⚙️ 프로젝트 설정

### 1) 사전 요구사항

- Node.js 20+
- pnpm 8+
- Java 17
- PostgreSQL, Redis

### 2) 백엔드 실행

`@backend/src/main/resources/application.properties.example`를 기준으로 로컬 설정 파일을 구성합니다.  
DB, Redis, OAuth, JWT 관련 환경변수를 주입해 실행합니다.  
기본 포트: **8080**

```bash
cd @backend
cp src/main/resources/application.properties.example src/main/resources/application-local.properties
./gradlew bootRun
```

### 3) 프론트엔드 실행

환경 설정: `@frontend/.env.local`

```env
NEXT_PUBLIC_BACKEND_URL=http://moa.hee-factory.com
```

기본 포트: **3000**

```bash
cd @frontend
pnpm install
pnpm dev
```

---

## 👉 개발 가이드라인

### 코드 컨벤션

- 파일/컴포넌트: PascalCase
- 유틸리티/훅: camelCase
- 함수/변수: camelCase
- 상수: UPPER_SNAKE_CASE

### Git 워크플로우

- **브랜치**
  - `main`: 최종 배포
  - `develop`: 통합 개발
  - `feature/*`: 기능 개발
  - `fix/*`: 버그 수정
- **커밋**
  - Conventional Commits 권장 (`feat`, `fix`, `refactor`, `chore`, `docs`, `style`)
  - 범위 태그 사용: `[ALL]`, `[FE]`, `[BE]`

---

## 📚 기타

- 프론트엔드 상세 안내: [@frontend/README.md](@frontend/README.md)
- 백엔드 상세 안내: [@backend/README.md](@backend/README.md)

> **서버 운영 시간**: AWS 비용 절감을 위해 **평일 09:00 ~ 21:00 (KST)** 에만 서버가 자동으로 가동됩니다.  
> 해당 시간 외 접속 시 응답이 없을 수 있습니다.
