package com.moa2.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonDataParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * JSON 문자열을 파싱하고 유효성 검증을 수행합니다.
     *
     * @param json  JSON 문자열
     * @param clazz 대상 클래스
     * @param <T>   대상 타입
     * @return 파싱 및 검증된 객체
     * @throws IllegalArgumentException 파싱 또는 검증 실패 시
     */
    public <T> T parseAndValidate(String json, Class<T> clazz) {
        try {
            // JSON 파싱
            T dto = objectMapper.readValue(json, clazz);

            // 유효성 검증
            Set<ConstraintViolation<T>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                log.error("Validation 실패: {}", errorMessage);
                throw new IllegalArgumentException("Validation failed: " + errorMessage);
            }

            return dto;
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
            log.debug("JSON 내용: {}", json);
            throw new IllegalArgumentException("JSON 파싱 실패: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("예상치 못한 오류: {}", e.getMessage(), e);
            throw new IllegalArgumentException("요청 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
