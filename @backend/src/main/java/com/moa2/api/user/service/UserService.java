package com.moa2.api.user.service;

import com.moa2.api.auth.dto.AuthDto;
import com.moa2.api.user.dto.UserDto;
import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.model.ReservationStatus;
import com.moa2.api.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 관련 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final RefreshTokenService refreshTokenService;

    /**
     * 내 정보 조회
     */
    @Transactional(readOnly = true)
    public AuthDto.UserInfoResponse getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        return AuthDto.UserInfoResponse.from(user);
    }

    /**
     * 회원 탈퇴
     * - 진행중인 예매가 있으면 탈퇴 불가
     * - RefreshToken 무효화
     * - Soft Delete 처리
     */
    @Transactional
    public UserDto.UserDeleteResponse deleteMyAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        // 1. 진행중인 예매 확인 (PENDING 또는 CONFIRMED 상태)
        List<Reservation> activeReservations = reservationRepository.findByUserAndStatusIn(
            user, 
            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
        );
        
        if (!activeReservations.isEmpty()) {
            log.warn("진행중인 예매가 있어 탈퇴할 수 없습니다: {} (예매 수: {})", email, activeReservations.size());
            throw new IllegalStateException("진행중인 예매가 있어 탈퇴할 수 없습니다. 예매를 취소한 후 다시 시도해주세요.");
        }
        
        // 2. RefreshToken 무효화
        try {
            refreshTokenService.deleteByUserEmailAndSocialProvider(email, user.getSocialProvider());
            log.info("RefreshToken 삭제 완료: {}", email);
        } catch (Exception e) {
            log.warn("RefreshToken 삭제 중 오류 (무시): {}", e.getMessage());
        }
        
        // 3. 회원 탈퇴 (Soft Delete)
        user.withdraw();
        userRepository.save(user);
        
        log.info("회원 탈퇴 완료: {}", email);

        return new UserDto.UserDeleteResponse(
                email,
                "회원 탈퇴가 완료되었습니다."
        );
    }
}
