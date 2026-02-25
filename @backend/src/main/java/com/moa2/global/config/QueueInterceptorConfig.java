package com.moa2.global.config;

import com.moa2.global.interceptor.QueueReadyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * V1: 대기열 인터셉터 설정 (DB 기반)
 * V2에서는 QueueTokenInterceptorConfig을 사용
 */
@Profile("v1")
@Configuration
@RequiredArgsConstructor
public class QueueInterceptorConfig implements WebMvcConfigurer {

    private final QueueReadyInterceptor queueReadyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 좌석 배치도 조회/선점 API는 READY 상태 사용자만 접근 가능
        registry.addInterceptor(queueReadyInterceptor)
                .addPathPatterns(
                        "/api/v1/schedules/*/seats",
                        "/api/v1/schedules/*/seats/lock",
                        "/api/v1/schedules/*/seats/unlock");
    }
}
