package com.moa2.domain.auth.converter;

import com.moa2.global.util.EncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Refresh Token 암호화/복호화 컨버터
 * JPA 엔티티 저장 시 자동 암호화, 조회 시 자동 복호화
 * 
 * Spring Bean으로 등록하여 EncryptionUtil을 주입받음
 */
@Component
@Converter(autoApply = false)
@RequiredArgsConstructor
public class RefreshTokenConverter implements AttributeConverter<String, String> {

    private final EncryptionUtil encryptionUtil;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        
        return encryptionUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        
        return encryptionUtil.decrypt(dbData);
    }
}

