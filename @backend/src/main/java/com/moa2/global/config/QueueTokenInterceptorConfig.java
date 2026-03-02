package com.moa2.global.config;

import com.moa2.global.interceptor.QueueTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * V2: 대기열 토큰 인터셉터 설정 (Redis 기반)
 * - /api/v1/schedules/{id}/seats 경로에 Redis 토큰 검증 적용
 * - V1의 QueueReadyInterceptor(DB 기반)를 대체
 */
@Profile("v2")
@Configuration
@RequiredArgsConstructor
public class QueueTokenInterceptorConfig implements WebMvcConfigurer {

    private final QueueTokenInterceptor queueTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // V2: 좌석 조회 전 Redis 토큰 보유 여부 확인
        registry.addInterceptor(queueTokenInterceptor)
                .addPathPatterns("/api/v1/schedules/*/seats");
    }
}
