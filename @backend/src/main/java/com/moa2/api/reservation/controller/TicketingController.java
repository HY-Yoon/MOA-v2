package com.moa2.api.reservation.controller;

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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 티켓팅 전용 컨트롤러 (V2)
 * - [1단계] 좌석 선점: POST /reserve → Redis 선점만 (DB 없음) → 200
 * - [3단계] 결제 완료: PaymentController에서 처리 (기존 유지)
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/reservations")
@RequiredArgsConstructor
public class TicketingController {

    private final ReservationFacade reservationFacade;
    private final UserRepository userRepository;

    private static final String QUEUE_TOKEN_COOKIE = "QUEUE-TOKEN";

    @Value("${security.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${security.cookie.same-site:None}")
    private String cookieSameSite;

    /**
     * [1단계] 좌석 선점
     * - QUEUE-TOKEN 쿠키에서 토큰 추출
     * - Redis에 좌석 선점만 저장 (DB Write 없음)
     * - 선점 성공 시 QUEUE-TOKEN 쿠키 즉시 삭제 (토큰 소진)
     */
    @Operation(summary = "V2 좌석 선점", description = "Redis 기반 좌석 선점 API. DB Write 없이 즉시 200 응답합니다.")
    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @Valid @RequestBody ReservationDtoV2.ReserveRequest request) {

        try {
            Long userId = getAuthenticatedUserId();
            // 쿠키에서 QUEUE-TOKEN 추출
            String token = extractQueueTokenFromCookie(httpRequest);
            if (token == null || token.isBlank()) {
                log.warn("V2 좌석 선점 실패 - QUEUE-TOKEN 쿠키 누락");
                return ResponseEntity.badRequest()
                        .body(com.moa2.global.dto.ApiResponse.error(
                                "대기열 토큰이 없습니다. 대기열을 통해 입장해주세요.",
                                ErrorResponse.of(ErrorCode.QUEUE_EXPIRED)));
            }

            ReservationDtoV2.ReserveSeatResponse response = reservationFacade.reserve(token, userId, request);

            // 선점 성공 후 QUEUE-TOKEN 쿠키 즉시 삭제 (토큰 소진)
            expireQueueTokenCookie(httpResponse);

            return ResponseEntity.ok(com.moa2.global.dto.ApiResponse.success(response));

        } catch (SeatConflictException e) {
            log.warn("V2 좌석 선점 실패 (좌석 충돌): {}", e.getConflictSeatIds());
            return ResponseEntity.status(HttpStatus.CONFLICT)
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

    /**
     * 요청에서 QUEUE-TOKEN 쿠키 값을 추출
     */
    private String extractQueueTokenFromCookie(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (jakarta.servlet.http.Cookie cookie : cookies) {
            if (QUEUE_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * QUEUE-TOKEN 쿠키 즉시 만료 (토큰 소진 후 재사용 방지)
     */
    private void expireQueueTokenCookie(HttpServletResponse response) {
        ResponseCookie expireCookie = ResponseCookie.from(QUEUE_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expireCookie.toString());
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new IllegalStateException("인증이 필요합니다.");
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof UserPrincipal userPrincipal)) {
            throw new IllegalStateException("인증 정보(principal)이 올바르지 않습니다. 다시 로그인해주세요.");
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
