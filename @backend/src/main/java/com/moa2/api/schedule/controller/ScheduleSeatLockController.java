package com.moa2.api.schedule.controller;

import com.moa2.api.schedule.controller.docs.ScheduleSeatLockControllerDocs;
import com.moa2.api.schedule.dto.ScheduleDto;
import com.moa2.api.schedule.exception.SeatLockConflictException;
import com.moa2.api.schedule.exception.SeatNotFoundException;
import com.moa2.api.schedule.service.ScheduleSeatLockService;
import com.moa2.api.schedule.service.ScheduleSeatUnlockService;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@Profile("v1")
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleSeatLockController implements ScheduleSeatLockControllerDocs {

    private final ScheduleSeatLockService scheduleSeatLockService;
    private final ScheduleSeatUnlockService scheduleSeatUnlockService;
    private final UserRepository userRepository;

    @Override
    @PostMapping("/{scheduleId}/seats/lock")
    public ResponseEntity<ApiResponse<ScheduleDto.SeatLockResponse>> lockSeats(
            @PathVariable Long scheduleId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ScheduleDto.SeatLockRequest request) {
        try {
            Long userId = getAuthenticatedUserId();
            LocalDateTime expiresAt = scheduleSeatLockService.lockSeats(scheduleId, request.seatIds(), userId);

            // DTO에 맞게 응답 생성
            ScheduleDto.SeatLockResponse response = ScheduleDto.SeatLockResponse.builder()
                    .isSuccess(true)
                    .expiresAt(expiresAt)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (SeatLockConflictException e) {
            log.info("좌석 선점 충돌: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("이미 선택된 좌석입니다.", null)); // Data null 처리

        } catch (SeatNotFoundException e) {
            log.warn("좌석 선점 실패: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), null));

        } catch (IllegalArgumentException e) {
            log.warn("좌석 선점 요청 오류: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Override
    @PostMapping("/{scheduleId}/seats/unlock")
    public ResponseEntity<ApiResponse<ScheduleDto.SeatUnlockResponse>> unlockSeats(
            @PathVariable Long scheduleId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ScheduleDto.SeatUnlockRequest request) {
        try {
            Long userId = getAuthenticatedUserId();
            scheduleSeatUnlockService.unlockSeats(scheduleId, request.seatIds(), userId);

            // DTO에 맞게 응답 생성
            ScheduleDto.SeatUnlockResponse response = ScheduleDto.SeatUnlockResponse.builder()
                    .isSuccess(true)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (SeatLockConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage(), null));

        } catch (SeatNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), null));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
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
        return userRepository.findByEmailAndSocialProvider(email, socialProvider)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }
}