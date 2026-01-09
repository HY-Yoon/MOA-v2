# ===== Builder Stage =====
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Gradle 래퍼 및 빌드 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# 의존성 다운로드 (캐싱 최적화)
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# 소스 코드 복사
COPY src src

# JAR 빌드 (테스트 스킵)
RUN ./gradlew bootJar -x test --no-daemon

# ===== Runtime Stage =====
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 타임존 설정 (Asia/Seoul)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

# 애플리케이션 JAR 복사
COPY --from=builder /build/build/libs/*.jar app.jar

# 업로드 디렉토리 생성
RUN mkdir -p /app/uploads

# 포트 노출 (Koyeb이 PORT 환경변수로 재정의 가능)
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8081}/actuator/health || exit 1

# JVM 메모리 최적화 (Koyeb 무료 티어 OOM 방지)
ENTRYPOINT ["java", \
    "-Xms256m", \
    "-Xmx360m", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=70.0", \
    "-XX:+UseSerialGC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=Asia/Seoul", \
    "-jar", \
    "app.jar"]

