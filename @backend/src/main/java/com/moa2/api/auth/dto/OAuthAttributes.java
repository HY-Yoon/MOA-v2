package com.moa2.api.auth.dto;

import com.moa2.api.user.domain.entity.User;
import com.moa2.global.model.Gender;
import com.moa2.global.model.SocialProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Map;

/**
 * OAuth2 제공자로부터 받은 사용자 정보를 담는 DTO
 */
@Slf4j
@Getter
@Builder
public class OAuthAttributes {
    private String name;
    private String email;
    private String picture;
    private String phone;
    private Gender gender;
    private LocalDate birthDate;
    private String ageRange;
    private String providerId;
    private SocialProvider provider;

    /**
     * OAuth2 제공자별 응답을 OAuthAttributes로 변환
     * @param providerName 제공자 이름 (google, kakao, naver 등)
     * @param attributes OAuth2 제공자로부터 받은 사용자 정보
     * @return OAuthAttributes 객체
     */
    public static OAuthAttributes of(String providerName, Map<String, Object> attributes) {
        if ("google".equals(providerName)) {
            return ofGoogle(attributes);
        } else if ("naver".equals(providerName)) {
            return ofNaver(attributes);
        } else if ("kakao".equals(providerName)) {
            return ofKakao(attributes);
        }
        throw new IllegalArgumentException("지원하지 않는 제공자입니다: " + providerName);
    }

    /**
     * Google OAuth2 응답 파싱
     * @param attributes Google OAuth2 응답
     * @return OAuthAttributes 객체
     */
    private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .providerId((String) attributes.get("sub"))
                .provider(SocialProvider.GOOGLE)
                .build();
    }

    /**
     * Naver OAuth2 응답 파싱
     * @param attributes Naver OAuth2 응답 (response 키 안에 실제 데이터)
     * @return OAuthAttributes 객체
     */
    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
        // 네이버는 response 키 안에 실제 사용자 정보가 있음
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            throw new IllegalArgumentException("네이버 OAuth2 응답에 response 키가 없습니다.");
        }

        // id는 필수값
        String id = (String) response.get("id");
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("네이버 OAuth2 응답에 id가 없습니다.");
        }

        // email, name, profile_image, mobile은 선택적 (사용자가 제공 거부 가능)
        String email = (String) response.get("email");
        String name = (String) response.get("name");
        String profileImage = (String) response.get("profile_image");
        String mobile = (String) response.get("mobile"); // 네이버는 mobile 필드로 전화번호 제공
        String genderStr = (String) response.get("gender"); // "M" 또는 "F"
        String birthday = (String) response.get("birthday"); // "MM-DD" 형식
        String birthyear = (String) response.get("birthyear"); // "YYYY" 형식
        String age = (String) response.get("age"); // 연령대 (예: "20-29")

        // 디버깅: NAVER가 실제로 보내는 데이터 로깅 (민감정보 마스킹)
        log.info("[NAVER OAuth2] 받은 데이터 - id: {}, email: {}, name: {}, mobile: {}, gender: {}, birthday: {}, birthyear: {}, age: {}",
                id != null ? "***" + id.substring(Math.max(0, id.length() - 4)) : null,
                email != null ? email.replaceAll("(.{1,3})@", "***@") : null,
                name != null ? name.charAt(0) + "**" : null,
                mobile != null ? "***" : null,
                genderStr,
                birthday,
                birthyear,
                age);

        // email이 null이면 기본값 설정
        if (email == null || email.trim().isEmpty()) {
            email = "no-email-" + id + "@naver.local";
        }

        // name이 null이면 기본값 설정
        if (name == null || name.trim().isEmpty()) {
            name = "네이버 사용자";
        }

        // 성별 변환: "M" -> MALE, "F" -> FEMALE, 그 외 -> null
        Gender gender = null;
        if (genderStr != null) {
            if ("M".equals(genderStr) || "m".equals(genderStr)) {
                gender = Gender.MALE;
            } else if ("F".equals(genderStr) || "f".equals(genderStr)) {
                gender = Gender.FEMALE;
            }
        }

        // 생년월일 변환: birthyear + birthday -> LocalDate
        // 주의: NAVER는 사용자가 동의 화면에서 선택하지 않았더라도,
        // 이미 보유하고 있는 정보는 API 응답에 포함시킬 수 있습니다.
        LocalDate birthDate = null;
        if (birthyear != null && !birthyear.trim().isEmpty() && 
            birthday != null && !birthday.trim().isEmpty()) {
            try {
                // birthday는 "MM-DD" 형식, birthyear는 "YYYY" 형식
                String[] dateParts = birthday.split("-");
                if (dateParts.length == 2) {
                    int month = Integer.parseInt(dateParts[0]);
                    int day = Integer.parseInt(dateParts[1]);
                    int year = Integer.parseInt(birthyear);
                    birthDate = LocalDate.of(year, month, day);
                    log.info("[NAVER OAuth2] 생년월일 파싱 성공: {}-{}-{}", year, month, day);
                }
            } catch (Exception e) {
                log.warn("[NAVER OAuth2] 생년월일 파싱 실패 - birthday: {}, birthyear: {}, error: {}", 
                        birthday, birthyear, e.getMessage());
            }
        } else {
            log.info("[NAVER OAuth2] 생년월일 정보 없음 - birthday: {}, birthyear: {}", birthday, birthyear);
        }

        return OAuthAttributes.builder()
                .name(name)
                .email(email)
                .picture(profileImage)
                .phone(mobile)
                .gender(gender)
                .birthDate(birthDate)
                .ageRange(age) // 연령대 저장
                .providerId(id)
                .provider(SocialProvider.NAVER)
                .build();
    }

    /**
     * Kakao OAuth2 응답 파싱
     * @param attributes Kakao OAuth2 응답
     * @return OAuthAttributes 객체
     */
    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(Map<String, Object> attributes) {
        // 카카오는 id가 최상위 레벨에 있음 (Long 타입일 수 있음)
        Object idObj = attributes.get("id");
        String id;
        if (idObj instanceof Long) {
            id = String.valueOf((Long) idObj);
        } else if (idObj instanceof Integer) {
            id = String.valueOf((Integer) idObj);
        } else {
            id = (String) idObj;
        }
        
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("카카오 OAuth2 응답에 id가 없습니다.");
        }

        // kakao_account는 선택적 (사용자가 동의하지 않을 수 있음)
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        
        String email = null;
        String name = null;
        String profileImage = null;
        
        if (kakaoAccount != null) {
            // email 추출
            email = (String) kakaoAccount.get("email");
            
            // profile 추출
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                name = (String) profile.get("nickname");
                profileImage = (String) profile.get("profile_image_url");
            }
        }
        
        // email이 null이면 기본값 설정
        if (email == null || email.trim().isEmpty()) {
            email = "no-email-" + id + "@kakao.local";
        }
        
        // name이 null이면 기본값 설정
        if (name == null || name.trim().isEmpty()) {
            name = "카카오 사용자";
        }

        return OAuthAttributes.builder()
                .name(name)
                .email(email)
                .picture(profileImage)
                .providerId(id)
                .provider(SocialProvider.KAKAO)
                .build();
    }

    /**
     * OAuthAttributes를 User 엔티티로 변환
     * @return User 엔티티
     */
    public User toEntity() {
        return User.builder()
                .email(email)
                .name(name)
                .picture(picture)
                .phone(phone)
                .gender(gender)
                .birthDate(birthDate)
                .ageRange(ageRange)
                .providerId(providerId)
                .socialProvider(provider)
                .build();
    }
}

