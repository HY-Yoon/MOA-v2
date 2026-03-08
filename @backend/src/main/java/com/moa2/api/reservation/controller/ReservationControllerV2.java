package com.moa2.api.reservation.controller;

import com.moa2.api.reservation.controller.docs.ReservationControllerV2Docs;
import com.moa2.api.reservation.dto.ReservationDtoV2;
import com.moa2.api.reservation.exception.SeatConflictException;
import com.moa2.api.reservation.service.v2.ReservationFacade;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ErrorResponse;
import com.moa2.global.model.ErrorCode;
import java.util.List;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * V2: Redis 기반 예매 컨트롤러
 * - [1단계] 좌석 선점: POST /reserve → Redis 선점만 (DB 없음) → 200
 * - [2단계] 주문 생성: POST /order  → DB 저장 (Reservation + Payment) → 200
 * - [3단계] 결제 완료: PaymentController에서 처리 (기존 유지)
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
     * [1단계] 좌석 선점
     * - X-Queue-Token 헤더로 토큰 전달
     * - Redis에 좌석 선점만 저장 (DB Write 없음)
     * - 즉시 200 OK 응답
     */
    @Operation(summary = "V2 좌석 선점", description = "Redis 기반 좌석 선점 API. DB Write 없이 즉시 200 응답합니다.")
    @PostMapping("/reserve")
    @Override
    public ResponseEntity<?> reserve(
            @RequestHeader("X-Queue-Token") String token,
            @Valid @RequestBody ReservationDtoV2.ReserveRequest request) {

        try {
            Long userId = getAuthenticatedUserId();
            ReservationDtoV2.ReserveSeatResponse response = reservationFacade.reserve(token, userId, request);
            return ResponseEntity.ok(com.moa2.global.dto.ApiResponse.success(response));

        } catch (SeatConflictException e) {
            log.warn("V2 좌석 선점 실패 (좌석 충돌): {}", e.getConflictSeatIds());
            return ResponseEntity.badRequest()
                    .body(com.moa2.global.dto.ApiResponse.error(
                            e.getMessage(),
                            ErrorResponse.ofConflict(e.getConflictSeatIds())));

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            ErrorCode code = isQueueTokenError(msg) ? ErrorCode.QUEUE_EXPIRED : ErrorCode.BAD_REQUEST;
            log.warn("V2 좌석 선점 실패 ({}): {}", code, msg);
            return ResponseEntity.badRequest()
                    .body(com.moa2.global.dto.ApiResponse.error(msg, ErrorResponse.of(code)));

        } catch (IllegalStateException e) {
            log.warn("V2 좌석 선점 실패 (상태 오류): {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(com.moa2.global.dto.ApiResponse.error(e.getMessage(),
                            ErrorResponse.of(ErrorCode.BAD_REQUEST)));
        }
    }

    /**
     * [미리보기] 결제 페이지 진입 시 주문 상세 조회
     * - 선점한 좌석 상세 정보, 결제 금액, 예약자 정보를 반환
     */
    @Operation(summary = "V2 주문 미리보기", description = "결제 화면에서 필요한 주문 상세 정보(공연명, 가격, 남은 시간 등)를 조회합니다.")
    @GetMapping("/preview")
    public ResponseEntity<?> getPreview(
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("scheduleSeatIds") List<Long> scheduleSeatIds) {

        try {
            Long userId = getAuthenticatedUserId();
            ReservationDtoV2.PreviewResponse response = reservationFacade.getPreviewInfo(userId, scheduleId, scheduleSeatIds);
            return ResponseEntity.ok(com.moa2.global.dto.ApiResponse.success(response));

        } catch (IllegalStateException e) {
            log.warn("V2 주문 미리보기 실패 (상태 오류): {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(com.moa2.global.dto.ApiResponse.error(e.getMessage(),
                            ErrorResponse.of(ErrorCode.BAD_REQUEST)));
        } catch (IllegalArgumentException e) {
            log.warn("V2 주문 미리보기 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(com.moa2.global.dto.ApiResponse.error(e.getMessage(),
                            ErrorResponse.of(ErrorCode.BAD_REQUEST)));
        }
    }

    /**
     * [2단계] 주문 생성
     * - 예약자 정보를 받아 DB에 Reservation + Payment(PENDING) 생성
     * - orderId를 반환하여 Toss 위젯 초기화
     */
    @Operation(summary = "V2 주문 생성", description = "예약자 정보를 입력받아 주문을 생성합니다. Toss 결제 위젯 초기화에 필요한 orderId를 반환합니다.")
    @PostMapping("/order")
    @Override
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody ReservationDtoV2.CreateOrderRequest request) {

        try {
            Long userId = getAuthenticatedUserId();
            ReservationDtoV2.CreateOrderResponse response = reservationFacade.createOrder(userId, request);
            return ResponseEntity.ok(com.moa2.global.dto.ApiResponse.success(response));

        } catch (IllegalStateException e) {
            // 선점 만료 또는 인증 오류
            log.warn("V2 주문 생성 실패 (상태 오류): {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(com.moa2.global.dto.ApiResponse.error(e.getMessage(),
                            ErrorResponse.of(ErrorCode.BAD_REQUEST)));

        } catch (IllegalArgumentException e) {
            log.warn("V2 주문 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(com.moa2.global.dto.ApiResponse.error(e.getMessage(),
                            ErrorResponse.of(ErrorCode.BAD_REQUEST)));
        }
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
