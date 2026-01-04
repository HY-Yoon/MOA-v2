package com.moa2.global.service;

import com.moa2.domain.user.entity.User;
import com.moa2.domain.user.repository.UserRepository;
import com.moa2.global.dto.OAuthAttributes;
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

import java.util.Collections;
import java.util.Map;

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

            // OAuth2 응답을 OAuthAttributes로 변환
            OAuthAttributes attributes = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());

            // DB에 사용자 저장 또는 업데이트
            User user = saveOrUpdate(attributes);

            log.info("OAuth2 로그인 성공: {} ({})", LogMaskingUtil.maskEmail(user.getEmail()), user.getSocialProvider());

            // DefaultOAuth2User 생성 (Spring Security가 인증 정보로 사용)
            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                    oAuth2User.getAttributes(),
                    userNameAttributeName
            );
            
        } catch (OAuth2AuthenticationException e) {
            // OAuth2 인증 예외는 그대로 전파
            OAuth2Error error = e.getError();
            log.error("OAuth2 인증 실패 [{}]: ErrorCode={}, Description={}", 
                    registrationId, 
                    error.getErrorCode(), 
                    LogMaskingUtil.mask(error.getDescription()));
            throw e;
            
        } catch (Exception e) {
            // 예상치 못한 예외 (네트워크 오류, JSON 파싱 오류 등)
            log.error("Google API 호출 실패 [{}]: {}", registrationId, e.getMessage(), e);
            
            // OAuth2AuthenticationException으로 변환
            throw new OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                            "server_error",
                            "Google API 호출 중 오류가 발생했습니다: " + e.getMessage(),
                            null
                    ),
                    e
            );
        }
    }
    

    /**
     * 사용자를 DB에 저장하거나 업데이트
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
            // 기존 사용자 정보 업데이트
            // DB에 이미 정보가 있으면 유지, 없으면 OAuth 정보 사용
            String oldName = user.getName();
            String oldPicture = user.getPicture();
            String oldPhone = user.getPhone();
            
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
            
            // 변경사항이 있을 때만 업데이트
            boolean nameChanged = !newName.equals(oldName);
            boolean pictureChanged = (newPicture != null && !newPicture.equals(oldPicture)) 
                    || (newPicture == null && oldPicture != null);
            boolean phoneChanged = (newPhone != null && !newPhone.equals(oldPhone))
                    || (newPhone == null && oldPhone != null);
            
            if (nameChanged || pictureChanged || phoneChanged) {
                user.updateOAuth2Info(newName, newPicture, newPhone);
                user = userRepository.save(user);
                log.info("기존 사용자 정보 업데이트: {} (name: {}, picture: {}, phone: {})", 
                        user.getEmail(), newName, newPicture, 
                        newPhone != null ? LogMaskingUtil.mask(newPhone) : "null");
            }
        }

        return user;
    }
}

