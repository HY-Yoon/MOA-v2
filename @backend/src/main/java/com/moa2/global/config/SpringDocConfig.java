package com.moa2.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SpringDocConfig {

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
}
