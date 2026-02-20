package com.moa2.api.reservation.controller;

import com.moa2.api.reservation.controller.docs.ReservationControllerV2Docs;
import com.moa2.api.reservation.dto.ReservationDtoV2;
import com.moa2.api.reservation.service.v2.ReservationFacade;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.context.annotation.Profile;

/**
 * V2: Redis 기반 예매 컨트롤러
 * - 토큰 기반 입장 제어
 * - Redisson 분산 락으로 동시성 제어
 */
@Slf4j
@Profile("v2")
@Tag(name = "예매 API V2", description = "Redis 기반 공연 예매 API (V2)")
@RestController
@RequestMapping("/api/v2/reservations")
@RequiredArgsConstructor
public class ReservationControllerV2 implements ReservationControllerV2Docs {

    private final ReservationFacade reservationFacade;
    private final UserRepository userRepository;

    /**
     * 좌석 예매
     * - X-Queue-Token 헤더로 토큰 전달
     */
    @Override
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<ReservationDtoV2.ReserveResponse>> reserve(
            @RequestHeader("X-Queue-Token") String token,
            @Valid @RequestBody ReservationDtoV2.ReserveRequest request) {

        try {
            Long userId = getAuthenticatedUserId();
            ReservationDtoV2.ReserveResponse response = reservationFacade.reserve(token, userId, request);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            log.warn("V2 예매 실패 (잘못된 요청): {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("V2 예매 실패 (동시성): {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // --- Private Methods ---

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new IllegalStateException("인증이 필요합니다.");
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof UserPrincipal userPrincipal)) {
            throw new IllegalStateException("인증 정보(principal)가 올바르지 않습니다. 다시 로그인해주세요.");
        }

        String email = userPrincipal.getEmail();
        String provider = userPrincipal.getProvider();
        if (provider == null || provider.isBlank()) {
            throw new IllegalStateException("인증 정보(provider)가 없습니다. 다시 로그인해주세요.");
        }

        SocialProvider socialProvider = SocialProvider.valueOf(provider);
        User user = userRepository.findByEmailAndSocialProvider(email, socialProvider)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return user.getId();
    }
}
