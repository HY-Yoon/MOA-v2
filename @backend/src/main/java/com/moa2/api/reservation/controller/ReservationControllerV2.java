package com.moa2.api.reservation.controller;

import com.moa2.api.reservation.controller.docs.ReservationControllerV2Docs;
import com.moa2.api.reservation.dto.ReservationDtoV2;
import com.moa2.api.reservation.exception.SeatConflictException;
import com.moa2.api.reservation.service.v2.ReservationFacade;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ErrorResponse;
import com.moa2.global.model.ErrorCode;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * V2: Redis 기반 예매 컨트롤러
 * - 토큰 기반 입장 제어
 * - Redisson 분산 락으로 동시성 제어
 *
 * 에러 응답 data 구조:
 * - QUEUE_EXPIRED : { "code": "QUEUE_EXPIRED" }
 * - BAD_REQUEST : { "code": "BAD_REQUEST" }
 * - SEAT_CONFLICT : { "code": "SEAT_CONFLICT", "conflictSeatIds": ["A-4",
 * "B-7"] }
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
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 좌석 예매
     * - X-Queue-Token 헤더로 토큰 전달
     */
    @Operation(summary = "V2 좌석 예매", description = "Redis 기반 예매 API입니다. 비동기 처리를 위해 202 Accepted를 반환하고, 상태 확인용 eventId를 제공합니다.")
    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(
            @RequestHeader("X-Queue-Token") String token,
            @Valid @RequestBody ReservationDtoV2.ReserveRequest request) {

        try {
            Long userId = getAuthenticatedUserId();
            ReservationDtoV2.ReserveAcceptedResponse response = reservationFacade.reserve(token, userId, request);
            // Kafka 비동기 처리: 202 Accepted 응답
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(com.moa2.global.dto.ApiResponse.success(response));

        } catch (SeatConflictException e) {
            // 좌석 충돌: 충돌 좌석 목록을 data에 포함
            log.warn("V2 예매 실패 (좌석 충돌): {}", e.getConflictSeatIds());
            return ResponseEntity.badRequest()
                    .body(com.moa2.global.dto.ApiResponse.error(
                            e.getMessage(),
                            ErrorResponse.ofConflict(e.getConflictSeatIds())));

        } catch (IllegalArgumentException e) {
            // 토큰 만료/사용됨 → QUEUE_EXPIRED, 존재하지 않는 좌석 → BAD_REQUEST 구분
            String msg = e.getMessage();
            ErrorCode code = isQueueTokenError(msg) ? ErrorCode.QUEUE_EXPIRED : ErrorCode.BAD_REQUEST;
            log.warn("V2 예매 실패 ({}): {}", code, msg);
            return ResponseEntity.badRequest()
                    .body(com.moa2.global.dto.ApiResponse.error(msg, ErrorResponse.of(code)));

        } catch (IllegalStateException e) {
            // 인증 오류 또는 기타 상태 오류
            log.warn("V2 예매 실패 (상태 오류): {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(com.moa2.global.dto.ApiResponse.error(e.getMessage(),
                            ErrorResponse.of(ErrorCode.BAD_REQUEST)));
        }
    }

    /**
     * 예매 처리 상태 폴링 API
     * - 클라이언트가 1~2초 간격으로 호출하여 비동기 처리 상태 확인
     * - PENDING: 아직 처리 중
     * - COMPLETED: DB 저장 완료 → 결제 페이지로 이동 가능
     * - FAILED: 처리 실패 → 재시도 안내
     */
    @Operation(summary = "예매 처리 상태 조회 (폴링)", description = "Kafka 비동기 처리 상태를 확인합니다.")
    @GetMapping("/status/{eventId}")
    public ResponseEntity<?> getReservationStatus(@PathVariable String eventId) {
        String statusKey = "reservation:status:" + eventId;
        String status = redisTemplate.opsForValue().get(statusKey);

        if (status == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(com.moa2.global.dto.ApiResponse.error(
                            "해당 예매 이벤트를 찾을 수 없습니다.",
                            Map.of("eventId", eventId, "status", "NOT_FOUND")));
        }

        if ("COMPLETED".equals(status)) {
            String resultKey = "reservation:result:" + eventId;
            String resultJson = redisTemplate.opsForValue().get(resultKey);

            if (resultJson != null) {
                try {
                    Object resultObj = objectMapper.readValue(resultJson, Object.class);
                    return ResponseEntity.ok(com.moa2.global.dto.ApiResponse.success(
                            Map.of("eventId", eventId, "status", status, "result", resultObj)));
                } catch (Exception e) {
                    log.error("예매 결과 JSON 파싱 실패: {}", e.getMessage());
                }
            }
        }

        return ResponseEntity.ok(com.moa2.global.dto.ApiResponse.success(
                Map.of("eventId", eventId, "status", status)));
    }

    /**
     * 토큰 관련 에러 메시지 여부 판별
     */
    private boolean isQueueTokenError(String message) {
        if (message == null)
            return false;
        return message.contains("토큰이 만료") ||
                message.contains("유효하지 않습니다") ||
                message.contains("이미 사용된 토큰") ||
                message.contains("토큰 소유자") ||
                message.contains("사용할 수 없는 토큰");
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
