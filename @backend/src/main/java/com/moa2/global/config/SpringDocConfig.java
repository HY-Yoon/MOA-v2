package com.moa2.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MOA v2 API Documentation")
                .description("MOA v2 백엔드 API 명세서")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("MOA Team")
                    .email("support@moa.com")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8081")
                    .description("로컬 개발 서버"),
                new Server()
                    .url("https://api.moa.com")
                    .description("프로덕션 서버")
            ));
    }
}

