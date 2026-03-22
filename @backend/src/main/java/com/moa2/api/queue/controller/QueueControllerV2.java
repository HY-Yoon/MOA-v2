package com.moa2.api.queue.controller;

import com.moa2.api.queue.controller.docs.QueueControllerV2Docs;
import com.moa2.api.queue.dto.QueueDtoV2;
import com.moa2.api.queue.service.v2.QueueServiceV2;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.QueueStatus;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.context.annotation.Profile;

/**
 * V2: Redis 기반 대기열 컨트롤러
 * - V1의 DB 기반 대기열을 대체
 * - Redis Sorted Set으로 빠른 순위 조회
 */
@Slf4j
@Profile("v2")
@Tag(name = "대기열 API V2", description = "Redis 기반 공연 예매 대기열 관리 API (V2)")
@RestController
@RequestMapping("/api/v2/queue")
@RequiredArgsConstructor
public class QueueControllerV2 implements QueueControllerV2Docs {

    private final QueueServiceV2 queueServiceV2;
    private final UserRepository userRepository;

    private static final String QUEUE_TOKEN_COOKIE = "QUEUE-TOKEN";
    private static final int QUEUE_TOKEN_TTL_SECONDS = 300; // Redis TTL과 동기화

    @Value("${security.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${security.cookie.same-site:None}")
    private String cookieSameSite;

    /**
     * 대기열 진입
     */
    @Override
    @PostMapping("/enter")
    public ResponseEntity<ApiResponse<QueueDtoV2.EnterResponse>> enterQueue(
            @Valid @RequestBody QueueDtoV2.EnterRequest request,
            HttpServletResponse httpServletResponse) {

        try {
            Long userId = getAuthenticatedUserId();
            QueueDtoV2.EnterResponse response = queueServiceV2.enterQueue(userId, request.scheduleId());

            // READY 상태 전환 시 token을 HttpOnly 쿠키로 발급
            if (response.status() == QueueStatus.READY && response.token() != null) {
                issueQueueTokenCookie(httpServletResponse, response.token());
            }

            return ResponseEntity.ok(ApiResponse.success(response.withTokenMasked()));

        } catch (IllegalArgumentException e) {
            log.warn("V2 대기열 진입 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 대기 상태 조회
     */
    @Override
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<QueueDtoV2.StatusResponse>> getQueueStatus(
            @RequestParam Long scheduleId,
            HttpServletResponse httpServletResponse) {

        try {
            Long userId = getAuthenticatedUserId();
            QueueDtoV2.StatusResponse response = queueServiceV2.getQueueStatus(userId, scheduleId);

            // READY 상태 전환 시 token을 HttpOnly 쿠키로 발급
            if (response.status() == QueueStatus.READY && response.token() != null) {
                issueQueueTokenCookie(httpServletResponse, response.token());
            }

            return ResponseEntity.ok(ApiResponse.success(response.withTokenMasked()));

        } catch (IllegalArgumentException e) {
            log.warn("V2 대기 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // --- Private Methods ---

    /**
     * READY 상태 전환 시 QUEUE-TOKEN 을 HttpOnly 쿠키로 발급
     */
    private void issueQueueTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(QUEUE_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(QUEUE_TOKEN_TTL_SECONDS)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

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
