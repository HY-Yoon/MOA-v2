package com.moa2.api.schedule.controller;

import com.moa2.api.schedule.dto.ScheduleDto;
import com.moa2.api.schedule.exception.SeatLockConflictException;
import com.moa2.api.schedule.service.ScheduleSeatLockService;
import com.moa2.api.schedule.service.ScheduleSeatUnlockService;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 회차 좌석 선점(LOCK) 컨트롤러
 * - 대기열 READY 상태 사용자만 접근 가능 (QueueReadyInterceptor에서 검증)
 */
@Slf4j
@Tag(name = "회차 좌석 선점 API", description = "회차별 좌석 선점(LOCK) API")
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleSeatLockController {

    private final ScheduleSeatLockService scheduleSeatLockService;
    private final ScheduleSeatUnlockService scheduleSeatUnlockService;
    private final UserRepository userRepository;

    @Operation(summary = "좌석 선점", description = "'선택하기'를 누르는 순간 특정 회차(scheduleId)의 좌석을 5분간 선점(LOCK)합니다.\n\n" +
            "- 동시성 제어: schedule_seats를 SELECT ... FOR UPDATE로 잠금\n" +
            "- 규칙: 요청 좌석이 모두 AVAILABLE일 때만 LOCKED로 변경\n" +
            "- 권한: 대기열 READY + 만료 전 사용자만 가능(Interceptor에서 검증)")
    @PostMapping("/{scheduleId}/seats/lock")
    public ResponseEntity<ApiResponse<?>> lockSeats(
            @Parameter(description = "스케줄 ID", required = true) @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleDto.SeatLockRequest request) {
        try {
            Long userId = getAuthenticatedUserId();
            LocalDateTime expiresAt = scheduleSeatLockService.lockSeats(scheduleId, request.seatIds(), userId);

            ScheduleDto.SeatLockResponse response = ScheduleDto.SeatLockResponse.builder()
                    .isSuccess(true)
                    .expiresAt(expiresAt)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (SeatLockConflictException e) {
            // 하나라도 AVAILABLE이 아니면 409 Conflict
            log.info("좌석 선점 충돌: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("이미 선택된 좌석입니다.", "SEAT_CONFLICT", e.getConflictSeatIds()));

        } catch (com.moa2.api.schedule.exception.SeatNotFoundException e) {
            log.warn("좌석 선점 실패: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST", e.getNotFoundSeatIds()));

        } catch (IllegalArgumentException e) {
            log.warn("좌석 선점 요청 오류: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST", null));
        }
    }

    @Operation(summary = "좌석 선점 해제", description = "특정 회차(scheduleId)에서 사용자가 선점한 좌석을 즉시 해제(UNLOCK)합니다.\n\n" +
            "- **사용 시점:** 결제 취소/뒤로가기/결제 실패 등\n" +
            "- **동시성 제어:** schedule_seats를 SELECT ... FOR UPDATE로 잠금\n" +
            "- **규칙:** 내 좌석(LOCKED + lockedByUserId 일치)만 해제 가능\n" +
            "- **권한:** 대기열 READY + 만료 전 사용자만 가능(Interceptor에서 검증)")
    @PostMapping("/{scheduleId}/seats/unlock")
    public ResponseEntity<ApiResponse<?>> unlockSeats(
            @Parameter(description = "스케줄 ID", required = true) @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleDto.SeatUnlockRequest request) {
        try {
            Long userId = getAuthenticatedUserId();
            scheduleSeatUnlockService.unlockSeats(scheduleId, request.seatIds(), userId);

            ScheduleDto.SeatUnlockResponse response = ScheduleDto.SeatUnlockResponse.builder()
                    .isSuccess(true)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (SeatLockConflictException e) {
            log.info("좌석 선점 해제 충돌: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage(), "SEAT_CONFLICT", e.getConflictSeatIds()));

        } catch (com.moa2.api.schedule.exception.SeatNotFoundException e) {
            log.warn("좌석 선점 해제 실패: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST", e.getNotFoundSeatIds()));

        } catch (IllegalArgumentException e) {
            log.warn("좌석 선점 해제 요청 오류: scheduleId={}, error={}", scheduleId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST", null));
        }
    }

    /**
     * SecurityContext에서 인증된 사용자의 ID를 가져옴
     * - QueueReadyInterceptor에서도 인증을 검증하지만, lockedByUserId 저장을 위해 여기서 userId가 필요함
     */
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
            // 과거 토큰( provider claim 없음 ) 또는 비정상 인증 상태
            throw new IllegalStateException("인증 정보(provider)가 없습니다. 다시 로그인해주세요.");
        }

        SocialProvider socialProvider = SocialProvider.valueOf(provider);
        User user = userRepository.findByEmailAndSocialProvider(email, socialProvider)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return user.getId();
    }
}
