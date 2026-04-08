# MOA v2 — 공연 예매 서비스 백엔드

> 대용량 트래픽 처리를 위한 Redis 대기열 / 좌석 선점, Kafka 비동기 알림, AWS 인프라 자동화를 적용한 공연 예매 플랫폼 백엔드

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [API 문서](#api-문서)
- [핵심 구현](#핵심-구현)
- [프로젝트 핵심 성과](#프로젝트-핵심-성과)
- [성능 테스트 결과 (k6)](#성능-테스트-결과-k6)
- [설치 및 실행](#설치-및-실행)
- [환경 변수](#환경-변수)

---

## 프로젝트 소개

**MOA**는 공연 예매 서비스입니다.  
인기 공연 오픈 시 발생하는 **순간 대용량 트래픽**을 안정적으로 처리하기 위해 다음 전략을 적용했습니다.

- **V1**: DB 기반 좌석 락 + DB 대기열 (기본 플로우)
- **V2**: Redis Sorted Set 대기열 + Redis 좌석 선점 + Redisson 분산 락 (고성능 플로우)

두 버전을 Spring Profile(`v1` / `v2`)로 분리하여 부하 테스트와 성능 비교를 수행했습니다.

---

## 주요 기능

### 공연 / 스케줄 관리

- 공연 등록·수정·삭제 (관리자)
- 회차(ShowSchedule)별 좌석 등급 / 가격 관리
- Canvas 기반 좌석맵 JSON 저장 (PostgreSQL `jsonb`)

### 대기열 시스템

|                  | V1 (DB)         | V2 (Redis)                                      |
| ---------------- | --------------- | ----------------------------------------------- |
| 대기 저장        | `queues` 테이블 | Redis Sorted Set (`waiting:queue:{scheduleId}`) |
| 활성 스케줄 관리 | DB              | Redis Set (`active:schedules`)                  |
| 중복 실행 방지   | —               | ShedLock + Redis                                |
| 배치 처리        | —               | Pipeline으로 레이턴시 최소화                    |

### 좌석 선점 / 예매

- **V1**: `ScheduleSeat` DB row 락 + Pessimistic Lock
- **V2**: Redis `SET NX TTL` 선점 → DB Write 없이 1단계 완료, Redisson 분산 락

### 결제 (Toss Payments 연동)

- 결제 요청 → 승인 → 완료 플로우
- Kafka `payment-notification` 토픽으로 이메일 알림 비동기 발송
- Consumer 수동 Ack + DLQ(FixedBackOff 3회) 설정

### 인증 / 보안

- OAuth 2.0 소셜 로그인
- JWT Access / Refresh Token (Refresh Token → Redis 저장)
- 대기열 토큰(`QUEUE-TOKEN` 쿠키) Filter / Interceptor 검증

### AWS 인프라 (Terraform)

- VPC + 프라이빗 서브넷 + NAT EC2 (역프록시)
- Auto Scaling Group (평일 업무시간 자동 스케줄)
- S3 + CloudFront CDN
- Route53 도메인 관리

---

## 기술 스택

| 분류           | 기술                                       |
| -------------- | ------------------------------------------ |
| Language       | Java 17                                    |
| Framework      | Spring Boot 3.4.1                          |
| ORM            | Spring Data JPA (Hibernate)                |
| DB             | PostgreSQL                                 |
| Cache / 대기열 | Redis (Spring Data Redis)                  |
| 분산 락        | Redisson 3.24.3                            |
| 스케줄 락      | ShedLock 5.9.1                             |
| 메시징         | Apache Kafka (spring-kafka)                |
| 보안           | Spring Security, OAuth2, JWT (jjwt 0.12.3) |
| 스토리지       | AWS S3 (SDK v2 2.25.11)                    |
| API 문서       | springdoc-openapi 2.7.0                    |
| 빌드           | Gradle Kotlin DSL                          |
| 컨테이너       | Docker, Docker Compose                     |
| IaC            | Terraform                                  |
| 부하 테스트    | k6                                         |
| 모니터링       | Prometheus + Grafana, Spring Actuator      |

---

## 시스템 아키텍처

```
[Client]
   │
   ▼
[Route53] → [NAT EC2 / Nginx Reverse Proxy]
                        │
              [Private Subnet]
                        │
            [ASG — Spring Boot App]
            ┌───────────┴────────────┐
         [PostgreSQL]   [Redis]   [Kafka (Aiven)]
                                       │
                              [Email Notification Consumer]
```

**S3 + CloudFront**: 정적 에셋 / 이미지 CDN  
**ShedLock**: 멀티 인스턴스 환경에서 대기열 스케줄러 단일 실행 보장

---

## API 문서

```
http://moa.hee-factory.com/swagger-ui/index.html
```

> **운영 시간 안내**: AWS 비용 절감을 위해 서버는 **평일 09:00 ~ 21:00 (KST)** 에만 자동으로 가동됩니다.  
> 해당 시간 외에는 접속이 되지 않을 수 있습니다. (EventBridge Scheduler로 EC2 · ASG 자동 시작/중지)

### 주요 엔드포인트

| Method | Path                                        | 설명                  |
| ------ | ------------------------------------------- | --------------------- |
| `POST` | `/api/auth/exchange-code`                   | OAuth 코드 교환       |
| `POST` | `/api/auth/refresh`                         | 토큰 갱신             |
| `GET`  | `/api/v1/shows`                             | 공연 목록             |
| `GET`  | `/api/v1/shows/{showId}`                    | 공연 상세             |
| `GET`  | `/api/v1/schedules/{scheduleId}/seats`      | 좌석 현황             |
| `POST` | `/api/v1/schedules/{scheduleId}/seats/lock` | 좌석 선점 (V1)        |
| `POST` | `/api/v2/reservations/reserve`              | 좌석 선점 (V2, Redis) |
| `GET`  | `/api/v2/reservations/preview`              | 예매 미리보기         |
| `POST` | `/api/v1/payment/request`                   | 결제 요청             |
| `POST` | `/api/v1/payment/confirm`                   | 결제 승인             |
| `POST` | `/api/v2/queue/enter`                       | 대기열 진입 (V2)      |
| `GET`  | `/api/v2/queue/status`                      | 대기열 상태           |

---

## 핵심 구현

### 1. 대기열 시스템 (V1 → V2)

인기 공연 예매 시 동시 접속자를 순차적으로 처리하기 위한 대기열을 두 버전으로 구현하고 성능을 비교했습니다.

|                | V1 (DB 기반)    | V2 (Redis 기반)                                 |
| -------------- | --------------- | ----------------------------------------------- |
| 대기 저장      | `queues` 테이블 | Redis Sorted Set (`waiting:queue:{scheduleId}`) |
| 활성 스케줄    | DB 조회         | Redis Set (`active:schedules`)                  |
| 중복 실행 방지 | —               | **ShedLock** + Redis (멀티 인스턴스 안전)       |
| 배치 처리      | —               | **Pipeline**으로 Redis 왕복 최소화              |

- Spring Profile(`v1` / `v2`)로 두 플로우를 분리하여 동일 도메인에서 비교 가능하도록 설계

### 2. 좌석 선점 · 동시성 제어

- **V1**: `ScheduleSeat` DB row Pessimistic Lock
- **V2**: Redis `SET NX TTL`로 선점 원자성 보장 → DB Write 없이 1단계 완료 (DB 부하 분산)
- **Redisson 분산 락**: 멀티 인스턴스 환경에서 선점 검증 구간 동시 접근 제어
- `QUEUE-TOKEN` 쿠키 기반 Filter / Interceptor로 대기 미완료 사용자 API 접근 차단

### 2-1. Redis 분산 락과 트랜잭션 생명주기 최적화 (V2 핵심)

- **문제**: 분산 락 획득 대기 시간(lock.tryLock)이 @Transactional 내부에 포함되어, 락을 기다리는 수많은 스레드가 불필요하게 DB 커넥션을 점유하는 병목 발생 (HikariCP 고갈).
- **해결**: 락 획득 후 실제 DB 쓰기가 필요한 시점에만 트랜잭션이 시작되도록 서비스 레이어 분리 (ReservationPersistService).
- **결과**: 동일한 커넥션 풀 자원으로 처리 가능한 동시성 수준을 10배 이상 향상.

### 3. Kafka 비동기 결제 알림

- 결제 완료 후 `payment-notification` 토픽으로 이벤트 발행
- Consumer에서 이메일 발송 처리 → 결제 API 응답 시간에서 알림 비용 분리
- **수동 Ack + DLQ(FixedBackOff 3회)** 로 메시지 유실 방지
- `Payment.isEmailSent` 필드로 재처리 시 중복 발송 방지

### 4. AWS 인프라 자동화 (Terraform)

- **네트워크**: VPC + 퍼블릭/프라이빗 서브넷 + NAT EC2(역프록시) → 백엔드 외부 노출 차단
- **컴퓨팅**: ASG + Launch Template (DB·Redis·Kafka 환경변수 UserData 주입, instance refresh)
- **CDN**: S3 + CloudFront + OAC 조합으로 이미지 서빙
- **DNS**: Route53 퍼블릭/프라이빗 Hosted Zone 분리 (외부 도메인 ↔ 내부 서비스 디스커버리)
- **비용 절감**: EventBridge Scheduler로 NAT EC2 · ASG를 평일 업무시간에만 가동

---

## 프로젝트 핵심 성과

- 응답 속도 개선: p95 Latency 50.79s → 100ms 미만 (약 500배 향상)
- 안정성 확보: 1,000 VUs 환경에서 시스템 에러율 0% 달성
- 자원 효율화: 트랜잭션 범위 최적화를 통해 DB 커넥션 점유 시간 95% 이상 단축

---

## 성능 테스트 결과 (k6)

100석 규모 공연 기준으로 V1(DB 좌석 락)과 V2(Redis 대기열 + 선점) 시나리오를 각각 측정했습니다.

### V1 — DB 좌석 락 (`test-scenario-seat-lock.js`, 100 VUs)

| 지표                 | 결과                   |
| -------------------- | ---------------------- |
| 동시 사용자          | 100 VUs                |
| p95 응답시간         | **1.29s**              |
| 시스템 에러 (500)    | **0%**                 |
| HTTP 실패율          | 1.74%                  |
| 좌석 선점 성공 (200) | 20% (경쟁 → 정상)      |
| 좌석 충돌 (409)      | 80% (동시 접근 → 정상) |

### V2 — Redis 대기열 + 선점 (`queue-flow.js`, 1000 VUs 점진 증가)

| 지표                      | 결과                                              |
| ------------------------- | ------------------------------------------------- |
| 동시 사용자               | **1,000 VUs** (V1 대비 10배)                      |
| p95 응답시간              | 3.99s                                             |
| 시스템 에러 (500)         | **0%**                                            |
| 대기열 진입 성공률        | **100%**                                          |
| 비즈니스 실패 (409, 매진) | 100% — 100석이 조기 소진된 이후의 요청, 정상 동작 |
| 총 처리 이터레이션        | 23,066 (5m 30s)                                   |

### V2 — Spike 테스트 (`spike-test.js`, 1000 VUs 급격 증가)

| 지표               | 결과                  |
| ------------------ | --------------------- |
| 동시 사용자        | 1,000 VUs (급격 증가) |
| p95 응답시간       | 26.69s                |
| 시스템 에러 (500)  | 88.66%                |
| DB 커넥션 이슈     | 1,568건               |
| 대기열 진입 성공률 | 54.21%                |

> Redis 대기열은 1,000명의 트래픽을 성공적으로 방어했으나, 최종 DB Write 단계에서 단일 인스턴스의 maximum-pool-size 한계로 인해 에러 발생.
> 결론: 현재 애플리케이션 아키텍처(Redis + Kafka)는 로직 최적화의 임계점에 도달함. 이후 1만 명 이상의 트래픽을 처리하기 위해서는 애플리케이션 수정이 아닌, 인프라의 수평적 확장(WAS Scale-out 및 DB Read Replica)이 필수적임을 수치로 증명함.

### 핵심 차이

1,000 VUs 이상의 초고부하 상황에서 발생한 에러는 로직의 결함이 아닌 단일 서버의 자원(CPU, DB Connection) 임계점임을 확인했습니다. 이는 애플리케이션 최적화 이후의 해결책은 **인프라의 수평적 확장(Scale-out)**에 있음을 시사합니다.

> **관련 기술 블로그**
> - [비관적 락(Pessimistic Lock)의 성능 한계 분석](https://hee-story6.tistory.com/234)
> - [k6 부하 테스트 결과 분석](https://hee-story6.tistory.com/237)
> - [kafka 도입기](https://hee-story6.tistory.com/238)
> - [AWS 인프라 구축 과정](https://hee-story6.tistory.com/242)
> - _링크 추가 예정_

### 테스트 시나리오

| 시나리오             | 파일                         | 목적                                    |
| -------------------- | ---------------------------- | --------------------------------------- |
| V1 좌석 락 기준      | `test-scenario-seat-lock.js` | V1 DB 락 방식 기준점 측정               |
| V2 Queue 전체 플로우 | `queue-flow.js`              | 대기열 진입 → 좌석 선점 → 예매 완료 E2E |
| V2 Spike (오픈런)    | `spike-test.js`              | 순간 최대 트래픽 내구성 및 병목 분석    |
| V2 Ramp-up (임계점)  | `ramp-up-test.js`            | 최대 처리량 임계점 측정                 |

---

## 설치 및 실행

### 요구사항

- Java 17+
- Docker & Docker Compose
- PostgreSQL, Redis (로컬 또는 도커)

### 로컬 실행 (Docker Compose)

```bash
# 인프라 컨테이너 실행 (PostgreSQL, Redis, Kafka)
docker-compose up -d

# 애플리케이션 실행 (V2 프로필)
./gradlew bootRun --args='--spring.profiles.active=local,v2'
```

### 부하 테스트용 더미 데이터 적용

```bash
# PostgreSQL에 더미 데이터 삽입 (1200석 규모)
docker exec -i <postgres-container> psql -U <user> -d <db> < src/main/resources/k6_dummy_data.sql
```

### 빌드

```bash
./gradlew build
```

---

## 환경 변수

로컬은 `application-local.properties`로 주입하고, 운영 환경(AWS)은 Terraform `templatefile`을 통해 Launch Template UserData로 자동 주입됩니다.

### DB

| 변수 | 설명 |
|------|------|
| `DB_HOST` | PostgreSQL 호스트 |
| `DB_PORT` | PostgreSQL 포트 |
| `DB_NAME` | 데이터베이스명 |
| `DB_USERNAME` | DB 사용자명 |
| `DB_PASSWORD` | DB 비밀번호 |

### Redis

| 변수 | 설명 |
|------|------|
| `REDIS_HOST` | Redis 호스트 |
| `REDIS_PORT` | Redis 포트 |
| `REDIS_PASSWORD` | Redis 비밀번호 |

### Kafka (Aiven SSL)

| 변수 | 설명 |
|------|------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 브로커 주소 |
| `KAFKA_SSL_KEYSTORE_CERT` | SSL 클라이언트 인증서 |
| `KAFKA_SSL_KEYSTORE_KEY` | SSL 클라이언트 개인키 |
| `KAFKA_SSL_TRUSTSTORE_CERT` | SSL CA 인증서 |

### 인증 / 보안

| 변수 | 설명 |
|------|------|
| `JWT_ACCESS_SECRET` | JWT Access Token 서명 키 |
| `JWT_REFRESH_SECRET` | JWT Refresh Token 서명 키 |
| `ENCRYPTION_KEY` | 암호화 키 |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 출처 |

### OAuth2 소셜 로그인

| 변수 | 설명 |
|------|------|
| `GOOGLE_CLIENT_ID` | Google OAuth2 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 클라이언트 시크릿 |
| `KAKAO_CLIENT_ID` | 카카오 OAuth2 클라이언트 ID |
| `NAVER_CLIENT_ID` | 네이버 OAuth2 클라이언트 ID |
| `NAVER_CLIENT_SECRET` | 네이버 OAuth2 클라이언트 시크릿 |

### 메일

| 변수 | 설명 |
|------|------|
| `MAIL_USERNAME` | 발신 메일 주소 |
| `MAIL_PASSWORD` | 메일 앱 비밀번호 |

### AWS

| 변수 | 설명 |
|------|------|
| `AWS_ACCESS_KEY_ID` | AWS 액세스 키 ID |
| `AWS_SECRET_ACCESS_KEY` | AWS 시크릿 액세스 키 |
