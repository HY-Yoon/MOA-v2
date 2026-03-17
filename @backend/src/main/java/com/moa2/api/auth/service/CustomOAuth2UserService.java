package com.moa2.api.auth.service;

import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.api.auth.dto.OAuthAttributes;
import com.moa2.global.model.Gender;
import com.moa2.global.model.UserStatus;
import com.moa2.global.util.LogMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;

/**
 * OAuth2 사용자 정보를 로드하고 DB에 저장/업데이트하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        try {
            // 기본 OAuth2UserService를 사용하여 사용자 정보 로드
            OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
            OAuth2User oAuth2User = delegate.loadUser(userRequest);

            // 제공자 이름 추출 (google, kakao 등)
            String userNameAttributeName = userRequest.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();

            // 카카오의 경우 접속 IP 로깅 (IP 제한 에러 디버깅용)
            if ("kakao".equals(registrationId)) {
                try {
                    // 카카오 API 호출 시 사용되는 IP 정보 로깅
                    log.info("[Kakao] OAuth2 사용자 정보 조회 성공 - ID: {}", oAuth2User.getAttributes().get("id"));
                    // IP는 서버의 아웃바운드 IP이므로 직접 확인 불가, 카카오 개발자 콘솔 로그에서 확인 필요
                } catch (Exception ex) {
                    log.debug("카카오 정보 로깅 중 오류 (무시)", ex);
                }
            }

            // OAuth2 응답을 OAuthAttributes로 변환
            OAuthAttributes attributes = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());

            // DB에 사용자 저장 또는 업데이트
            User user = saveOrUpdate(attributes);

            log.info("OAuth2 로그인 성공: {} ({})", LogMaskingUtil.maskEmail(user.getEmail()), user.getSocialProvider());

            // DefaultOAuth2User 생성 (Spring Security가 인증 정보로 사용)
            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                    oAuth2User.getAttributes(),
                    userNameAttributeName);

        } catch (OAuth2AuthenticationException e) {
            // OAuth2 인증 예외는 그대로 전파
            OAuth2Error error = e.getError();

            // 카카오의 경우 IP 제한 에러 체크
            if ("kakao".equals(registrationId) && error.getDescription() != null
                    && error.getDescription().contains("ip mismatched")) {
                log.error("OAuth2 인증 실패 [{}]: IP 제한 에러 - {}",
                        registrationId, error.getDescription());
            } else {
                log.error("OAuth2 인증 실패 [{}]: ErrorCode={}, Description={}, URI={}",
                        registrationId,
                        error.getErrorCode(),
                        LogMaskingUtil.mask(error.getDescription()),
                        error.getUri());
            }
            throw e;

        } catch (Exception e) {
            // 예상치 못한 예외 (네트워크 오류, JSON 파싱 오류 등)
            log.error("OAuth2 API 호출 실패 [{}]: {}", registrationId, e.getMessage(), e);

            // OAuth2AuthenticationException으로 변환
            throw new OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                            "server_error",
                            "OAuth2 API 호출 중 오류가 발생했습니다: " + e.getMessage(),
                            null),
                    e);
        }
    }

    /**
     * 사용자를 DB에 저장하거나 업데이트
     * 
     * @param attributes OAuth2 사용자 정보
     * @return 저장/업데이트된 User 엔티티
     */
    private User saveOrUpdate(OAuthAttributes attributes) {

        User user = userRepository
                .findBySocialProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                .orElse(null);

        if (user == null) {
            // 신규 사용자 저장
            user = attributes.toEntity();
            user = userRepository.save(user);
            log.info("신규 사용자 등록: {} (name: {}, picture: {})",
                    user.getEmail(), user.getName(), user.getPicture());
        } else {
            boolean statusRestored = false;
            // 로그인 시 무조건 ACTIVE 상태로 강제 변경 (요청사항: 강제탈퇴/정지된 사용자도 로그인하면 ACTIVE로 복구)
            if (user.getStatus() != UserStatus.ACTIVE) {
                user.activate();
                statusRestored = true;
                log.info("사용자 인증 - 상태를 ACTIVE로 강제 복구: {} ({})",
                        LogMaskingUtil.maskEmail(user.getEmail()), user.getSocialProvider());
            }

            // 기존 사용자 정보 업데이트
            // DB에 이미 정보가 있으면 유지, 없으면 OAuth 정보 사용
            String oldName = user.getName();
            String oldPicture = user.getPicture();
            String oldPhone = user.getPhone();
            Gender oldGender = user.getGender();
            LocalDate oldBirthDate = user.getBirthDate();
            String oldAgeRange = user.getAgeRange();

            // 이름: DB에 값이 있으면 유지, 없으면 OAuth 정보 사용
            String newName = (oldName != null && !oldName.trim().isEmpty())
                    ? oldName
                    : attributes.getName();

            // 프로필 이미지: OAuth 정보로 항상 업데이트 (최신 프로필 사진 반영)
            String newPicture = attributes.getPicture();

            // 전화번호: OAuth에서 제공되면 업데이트 (기존 값이 없거나 새 값이 있으면)
            String newPhone = attributes.getPhone();
            if (newPhone != null && !newPhone.trim().isEmpty() &&
                    (oldPhone == null || oldPhone.trim().isEmpty())) {
                // 새 전화번호가 있고 기존 전화번호가 없으면 업데이트
            } else if (newPhone == null || newPhone.trim().isEmpty()) {
                // OAuth에서 전화번호를 제공하지 않으면 기존 값 유지
                newPhone = oldPhone;
            }

            // 성별: OAuth에서 제공되면 업데이트 (기존 값이 없거나 새 값이 있으면)
            Gender newGender = attributes.getGender();
            if (newGender == null) {
                newGender = oldGender;
            } else if (oldGender == null) {
                // 새 성별이 있고 기존 성별이 없으면 업데이트
            } else {
                // 둘 다 있으면 기존 값 유지
                newGender = oldGender;
            }

            // 생년월일: OAuth에서 제공되면 업데이트 (기존 값이 없거나 새 값이 있으면)
            LocalDate newBirthDate = attributes.getBirthDate();
            if (newBirthDate == null) {
                newBirthDate = oldBirthDate;
            } else if (oldBirthDate == null) {
                // 새 생년월일이 있고 기존 생년월일이 없으면 업데이트
            } else {
                // 둘 다 있으면 기존 값 유지
                newBirthDate = oldBirthDate;
            }

            // 연령대: OAuth에서 제공되면 업데이트 (기존 값이 없거나 새 값이 있으면)
            String newAgeRange = attributes.getAgeRange();
            if (newAgeRange == null || newAgeRange.trim().isEmpty()) {
                newAgeRange = oldAgeRange;
            } else if (oldAgeRange == null || oldAgeRange.trim().isEmpty()) {
                // 새 연령대가 있고 기존 연령대가 없으면 업데이트
            } else {
                // 둘 다 있으면 기존 값 유지
                newAgeRange = oldAgeRange;
            }

            // 변경사항이 있을 때만 업데이트
            boolean nameChanged = !newName.equals(oldName);
            boolean pictureChanged = (newPicture != null && !newPicture.equals(oldPicture))
                    || (newPicture == null && oldPicture != null);
            boolean phoneChanged = (newPhone != null && !newPhone.equals(oldPhone))
                    || (newPhone == null && oldPhone != null);
            boolean genderChanged = (newGender != null && !newGender.equals(oldGender))
                    || (newGender == null && oldGender != null);
            boolean birthDateChanged = (newBirthDate != null && !newBirthDate.equals(oldBirthDate))
                    || (newBirthDate == null && oldBirthDate != null);
            boolean ageRangeChanged = (newAgeRange != null && !newAgeRange.equals(oldAgeRange))
                    || (newAgeRange == null && oldAgeRange != null);

            if (statusRestored || nameChanged || pictureChanged || phoneChanged || genderChanged || birthDateChanged || ageRangeChanged) {
                user.updateOAuth2Info(newName, newPicture, newPhone, newGender, newBirthDate, newAgeRange);
                user = userRepository.save(user);
                log.info(
                        "기존 사용자 정보 업데이트: {} (name: {}, picture: {}, phone: {}, gender: {}, birthDate: {}, ageRange: {}, statusRestored: {})",
                        user.getEmail(), newName, newPicture,
                        newPhone != null ? LogMaskingUtil.mask(newPhone) : "null",
                        newGender != null ? newGender : "null",
                        newBirthDate != null ? newBirthDate : "null",
                        newAgeRange != null ? newAgeRange : "null",
                        statusRestored);
            }
        }

        return user;
    }
}
