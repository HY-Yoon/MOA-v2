package com.moa2.global.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * OAuth2 클라이언트 설정
 * Google API 호출 시 타임아웃 설정
 */
@Configuration
public class OAuth2ClientConfig {

    /**
     * RestTemplate 빈 설정 (타임아웃 포함)
     * Google API 호출 시 사용
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))  // 연결 타임아웃: 5초
                .setReadTimeout(Duration.ofSeconds(10))   // 읽기 타임아웃: 10초
                .build();
    }

    /**
     * ClientHttpRequestFactory 설정 (타임아웃 포함)
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 연결 타임아웃: 5초
        factory.setReadTimeout(10000);   // 읽기 타임아웃: 10초
        return factory;
    }
}

