package com.moa2.global.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

        @Bean
        @Primary
        public ObjectMapper objectMapper() {
                JavaTimeModule javaTimeModule = new JavaTimeModule();
                // 날짜 포맷 강제 설정 (yyyy-MM-dd'T'HH:mm:ss)
                javaTimeModule.addSerializer(LocalDateTime.class,
                                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));

                return Jackson2ObjectMapperBuilder.json()
                                .modules(javaTimeModule)
                                .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                                .featuresToDisable(
                                                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                                                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .simpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") // Date 타입 지원
                                .build();
        }

        @Bean
        public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
                return builder -> builder
                                .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                                .featuresToDisable(
                                                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                                                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }

}
