package com.moa2.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;

@Configuration
public class SpringDocConfig {
    static {
        Schema<String> schema = new Schema<String>();
        schema.type("string");
        schema.example("2026-03-15T19:00:00");
        schema.description("yyyy-MM-dd'T'HH:mm:ss (KST, 타임존 없음)");
        SpringDocUtils.getConfig().replaceWithSchema(LocalDateTime.class, schema);
    }


    @Value("${APP_URL:http://localhost:8081}")
    private String appUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MOA v2 API Documentation")
                        .description("MOA v2 공연 예매 플랫폼 백엔드 API 명세서. OAuth2 소셜 로그인(Google, Kakao, Naver) 및 JWT 기반 인증.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("MOA Team")
                                .email("support@moa.com")))
                .servers(List.of(
                        new Server()
                                .url(appUrl)
                                .description("API 서버")))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Access Token을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.")));
    }

    /**
     * Swagger UI에서 LocalDateTime을 KST 기준 타임존 없이 표시하도록 설정
     */
    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper);
    }

    /**
     * Jackson ObjectMapper 설정 - KST 타임존, 타임존 표시 제거
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        
        // 타임존 설정
        builder.timeZone(TimeZone.getTimeZone("Asia/Seoul"));
        
        // LocalDateTime 포맷 설정 (타임존 없이)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        builder.modules(javaTimeModule);
        
        // Timestamp 대신 ISO-8601 형식 사용
        builder.simpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        
        return builder;
    }
}
