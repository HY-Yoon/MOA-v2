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
 */
@Configuration
@Profile("v2")
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        String address = "redis://" + host + ":" + port;

        config.useSingleServer()
                .setAddress(address)
                .setConnectionPoolSize(50)
                .setConnectionMinimumIdleSize(10)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        // 비밀번호가 설정된 경우
        if (password != null && !password.isBlank()) {
            config.useSingleServer().setPassword(password);
        }

        return Redisson.create(config);
    }
}
