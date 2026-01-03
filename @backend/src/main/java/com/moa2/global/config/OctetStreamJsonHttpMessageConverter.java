package com.moa2.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * application/octet-stream을 JSON으로 파싱하는 HttpMessageConverter
 * multipart/form-data에서 Content-Type이 명시되지 않은 JSON part를 처리
 */
public class OctetStreamJsonHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private final ObjectMapper objectMapper;

    public OctetStreamJsonHttpMessageConverter(ObjectMapper objectMapper) {
        super(MediaType.APPLICATION_OCTET_STREAM);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // MultipartFile이 아닌 모든 타입 지원
        return !org.springframework.web.multipart.MultipartFile.class.isAssignableFrom(clazz);
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) 
            throws IOException, HttpMessageNotReadableException {
        try {
            String content = new String(inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8);
            // JSON으로 파싱 시도
            return objectMapper.readValue(content, clazz);
        } catch (Exception e) {
            throw new HttpMessageNotReadableException(
                "JSON 파싱 실패: " + e.getMessage(), e, inputMessage);
        }
    }

    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage) 
            throws IOException, HttpMessageNotWritableException {
        // 쓰기는 지원하지 않음
        throw new UnsupportedOperationException("Write operation is not supported");
    }
}

