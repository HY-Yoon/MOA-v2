package com.moa2.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

/**
 * V2: Redis Configuration
 * - RedisAutoConfiguration is always excluded (see Moa2Application.java)
 * - This class manually creates Redis beans only when 'v2' profile is active
 * - 운영(prod): REDIS_URL 환경변수 (rediss://default:password@host:port)
 * - 로컬: redis://localhost:6379
 */
@Configuration
@Profile("v2")
public class RedisConfig {

    @Value("${REDIS_URL:redis://localhost:6379}")
    private String redisUrl;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        URI uri = URI.create(redisUrl);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(uri.getHost());
        config.setPort(uri.getPort());

        // URI에서 username:password 추출 (rediss://default:password@host:port)
        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            String[] parts = userInfo.split(":", 2);
            if (parts.length == 2) {
                config.setUsername(parts[0]);
                config.setPassword(parts[1]);
            } else {
                config.setPassword(parts[0]);
            }
        }

        // rediss:// 스킴이면 TLS(SSL) 활성화
        boolean useSsl = "rediss".equalsIgnoreCase(uri.getScheme());

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
                LettuceClientConfiguration.builder();
        if (useSsl) {
            clientConfigBuilder.useSsl().disablePeerVerification();
        }

        return new LettuceConnectionFactory(config, clientConfigBuilder.build());
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}
