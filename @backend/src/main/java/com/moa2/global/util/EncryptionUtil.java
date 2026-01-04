package com.moa2.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-256 암호화 유틸리티
 * Refresh Token 저장 시 암호화, 조회 시 복호화
 * Redis로 전환해도 동일하게 사용 가능
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    
    private final SecretKeySpec secretKey;

    public EncryptionUtil(@Value("${encryption.key}") String encryptionKey) {
        // 32바이트 (256비트) 키 생성
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        
        // 키 길이가 32바이트가 아니면 패딩 또는 잘라내기
        if (keyBytes.length != 32) {
            byte[] adjustedKey = new byte[32];
            if (keyBytes.length < 32) {
                // 키가 짧으면 반복하여 채움
                System.arraycopy(keyBytes, 0, adjustedKey, 0, keyBytes.length);
                for (int i = keyBytes.length; i < 32; i++) {
                    adjustedKey[i] = keyBytes[i % keyBytes.length];
                }
            } else {
                // 키가 길면 앞 32바이트만 사용
                System.arraycopy(keyBytes, 0, adjustedKey, 0, 32);
            }
            keyBytes = adjustedKey;
        }
        
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * 평문을 암호화
     * @param plainText 암호화할 평문
     * @return Base64로 인코딩된 암호문
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("암호화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("암호화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 암호문을 복호화
     * @param encryptedText Base64로 인코딩된 암호문
     * @return 복호화된 평문
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("복호화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("복호화 중 오류가 발생했습니다.", e);
        }
    }
}

