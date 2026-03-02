package com.moa2.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Profile;

/**
 * V2: Redisson 분산 락 설정
 * - 좌석 선점 시 분산 환경에서 동시성 제어
 * - 운영(prod): REDIS_URL 환경변수 (rediss://default:password@host:port)
 * - 로컬: redis://localhost:6379
 */
@Configuration
@Profile("v2")
public class RedissonConfig {

    @Value("${REDIS_URL:redis://localhost:6379}")
    private String redisUrl;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        config.useSingleServer()
                .setAddress(redisUrl)
                .setConnectionPoolSize(50)
                .setConnectionMinimumIdleSize(10)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        return Redisson.create(config);
    }
}
