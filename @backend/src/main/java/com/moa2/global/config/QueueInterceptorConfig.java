package com.moa2.global.config;

import com.moa2.global.interceptor.QueueReadyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 대기열 인터셉터 설정
 */
@Configuration
@RequiredArgsConstructor
public class QueueInterceptorConfig implements WebMvcConfigurer {

    private final QueueReadyInterceptor queueReadyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 좌석 배치도 조회 API는 READY 상태 사용자만 접근 가능
        registry.addInterceptor(queueReadyInterceptor)
                .addPathPatterns("/api/v1/schedules/*/seats");
    }
}

