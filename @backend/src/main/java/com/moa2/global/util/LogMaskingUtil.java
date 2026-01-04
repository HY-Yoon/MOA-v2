package com.moa2.global.util;

import java.util.regex.Pattern;

/**
 * 로그 출력 시 민감정보를 마스킹하는 유틸 클래스
 */
public class LogMaskingUtil {

    // 이메일 정규식
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^([^@]+)@(.+)$");

    /**
     * 토큰 마스킹 (앞 4자리만 표시)
     * @param token 토큰 문자열
     * @return 마스킹된 토큰 (예: "eyJh****")
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }

        if (token.length() <= 4) {
            return "****";
        }

        return token.substring(0, 4) + "****";
    }

    /**
     * 이메일 마스킹 (아이디 절반만 표시)
     * @param email 이메일 주소
     * @return 마스킹된 이메일 (예: "tes***@gmail.com")
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }

        var matcher = EMAIL_PATTERN.matcher(email);
        if (!matcher.matches()) {
            // 이메일 형식이 아니면 전체 마스킹
            return maskToken(email);
        }

        String localPart = matcher.group(1); // @ 앞부분
        String domain = matcher.group(2);     // @ 뒷부분

        // 아이디 절반만 표시
        int visibleLength = localPart.length() / 2;
        if (visibleLength == 0) {
            visibleLength = 1;
        }

        String maskedLocal = localPart.substring(0, visibleLength) + "***";
        return maskedLocal + "@" + domain;
    }

    /**
     * 이름 마스킹 (성만 표시)
     * @param name 이름
     * @return 마스킹된 이름 (예: "김**")
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // 한글 이름인 경우
        if (name.length() >= 2 && isKorean(name.charAt(0))) {
            return name.charAt(0) + "**";
        }

        // 영문 이름인 경우 첫 글자만 표시
        if (name.length() >= 2) {
            return name.charAt(0) + "***";
        }

        // 1글자인 경우
        return "***";
    }

    /**
     * 한글 여부 확인
     */
    private static boolean isKorean(char ch) {
        return ch >= 0xAC00 && ch <= 0xD7A3;
    }

    /**
     * 일반적인 민감 정보 마스킹 (기존 로직 유지)
     * 토큰 형식의 긴 문자열을 마스킹
     * @param info 민감 정보
     * @return 마스킹된 정보
     */
    public static String maskSensitiveInfo(String info) {
        if (info == null || info.isEmpty()) {
            return info;
        }

        // 토큰 형식 마스킹 (JWT 등)
        if (info.length() > 50) {
            return info.substring(0, 20) + "..." + info.substring(info.length() - 10);
        }

        return info;
    }

    /**
     * 통합 마스킹 메서드
     * 입력값의 타입을 자동으로 판단하여 적절한 마스킹 적용
     * @param value 마스킹할 값
     * @return 마스킹된 값
     */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 이메일 형식인 경우
        if (EMAIL_PATTERN.matcher(value).matches()) {
            return maskEmail(value);
        }

        // 토큰 형식인 경우 (긴 문자열)
        if (value.length() > 50) {
            return maskToken(value);
        }

        // 이름 형식인 경우 (짧은 문자열, 한글이 포함된 경우)
        if (value.length() <= 10 && value.chars().anyMatch(ch -> isKorean((char) ch))) {
            return maskName(value);
        }

        // 기본 마스킹
        return maskSensitiveInfo(value);
    }
}

