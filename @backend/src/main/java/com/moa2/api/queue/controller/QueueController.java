package com.moa2.api.queue.controller;

import com.moa2.api.queue.controller.docs.QueueControllerDocs;
import com.moa2.api.queue.dto.QueueDto;
import com.moa2.api.queue.service.QueueService;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
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
 * 대기열 API 컨트롤러
 * Cookie 기반 JWT 인증 사용
 */
@Slf4j
@Tag(name = "대기열 API", description = "공연 예매 대기열 관리 API")
@RestController
@Profile("v1")
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController implements QueueControllerDocs {

    private final QueueService queueService;
    private final UserRepository userRepository;

    /**
     * 대기열 진입 (줄 서기)
     */
    @Override
    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<QueueDto.EnterResponse>> enterQueue(
            @Valid @RequestBody QueueDto.EnterRequest request) {

        try {
            Long userId = getAuthenticatedUserId();
            QueueDto.EnterResponse response = queueService.enterQueue(userId, request);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            log.warn("대기열 진입 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 대기 상태 조회 구현
     */
    @Override
    @GetMapping("/tokens/status")
    public ResponseEntity<ApiResponse<QueueDto.StatusResponse>> getQueueStatus(
            @RequestParam Long scheduleId) {

        try {
            Long userId = getAuthenticatedUserId();
            QueueDto.StatusResponse response = queueService.getQueueStatus(userId, scheduleId);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            log.warn("대기 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // --- Private Methods (인증 관련 로직 유지) ---

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
