package com.moa2.api.queue.controller;

import com.moa2.api.queue.dto.QueueDtoV2;
import com.moa2.api.queue.service.v2.QueueServiceV2;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import com.moa2.global.security.UserPrincipal;
import com.moa2.global.token.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

/**
 * V2 대기열 상태 테스트용 디버그 컨트롤러
 * - local & v2 프로필에서만 노출
 * - 로그인한 사용자 기준으로 WAITING/READY 시나리오를 강제 구성
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/queue/debug")
@RequiredArgsConstructor
public class QueueDebugControllerV2 {

    private static final String USER_TOKEN_PREFIX = "user:token:";

    private final RedisTemplate<String, String> redisTemplate;
    private final QueueServiceV2 queueServiceV2;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    /**
     * 로그인 사용자 기준 WAITING 상태 강제 생성
     */
    @PostMapping("/scenarios/waiting")
    public ResponseEntity<ApiResponse<QueueDtoV2.DebugScenarioResponse>> makeWaitingScenario(
            @Valid @RequestBody QueueDtoV2.DebugWaitingScenarioRequest request) {
        Long userId = getAuthenticatedUserId();
        Long scheduleId = request.scheduleId();

        String queueKey = queueServiceV2.getQueueKey(scheduleId);
        String activeSchedulesKey = queueServiceV2.getActiveSchedulesKey();
        String currentUserIdStr = userId.toString();

        clearUserToken(userId, scheduleId);
        if (request.shouldClearExistingQueue()) {
            redisTemplate.delete(queueKey);
        } else {
            redisTemplate.opsForZSet().remove(queueKey, currentUserIdStr);
        }

        long usersAhead = request.normalizedUsersAhead();
        long baseScore = System.currentTimeMillis() - 100_000L;
        Set<ZSetOperations.TypedTuple<String>> waitingEntries = new HashSet<>();
        for (long i = 1; i <= usersAhead; i++) {
            // 스케줄러에서 Long.parseLong 처리하므로 숫자형 사용자 ID 형태로 주입
            String fakeUserId = String.valueOf(9_000_000_000L + i);
            waitingEntries.add(ZSetOperations.TypedTuple.of(fakeUserId, (double) (baseScore + i)));
        }
        waitingEntries.add(ZSetOperations.TypedTuple.of(currentUserIdStr, (double) System.currentTimeMillis()));
        redisTemplate.opsForZSet().add(queueKey, waitingEntries);
        if (request.shouldActivateScheduler()) {
            redisTemplate.opsForSet().add(activeSchedulesKey, String.valueOf(scheduleId));
        } else if (request.shouldClearExistingQueue()) {
            // 테스트용 WAITING 고정 상태를 원할 때는 스케줄러 처리 대상에서 제외
            redisTemplate.opsForSet().remove(activeSchedulesKey, String.valueOf(scheduleId));
        }

        Long rank = redisTemplate.opsForZSet().rank(queueKey, currentUserIdStr);
        long position = (rank != null) ? rank + 1 : usersAhead + 1;
        long totalWaiting = usersAhead + 1;

        log.info("Debug WAITING 시나리오 구성 완료 - userId={}, scheduleId={}, usersAhead={}, position={}, clearExistingQueue={}, activateScheduler={}",
                userId, scheduleId, usersAhead, position, request.shouldClearExistingQueue(), request.shouldActivateScheduler());

        String resultMessage = request.shouldActivateScheduler()
                ? "WAITING 시나리오 구성 완료 (스케줄러 활성)"
                : "WAITING 시나리오 구성 완료 (스케줄러 비활성)";

        return ResponseEntity.ok(ApiResponse.success(
                QueueDtoV2.DebugScenarioResponse.of(
                        resultMessage,
                        scheduleId,
                        userId,
                        position,
                        totalWaiting
                )
        ));
    }

    /**
     * 로그인 사용자 기준 READY 상태 강제 생성
     */
    @PostMapping("/scenarios/ready")
    public ResponseEntity<ApiResponse<QueueDtoV2.DebugScenarioResponse>> makeReadyScenario(
            @Valid @RequestBody QueueDtoV2.DebugReadyScenarioRequest request) {
        Long userId = getAuthenticatedUserId();
        Long scheduleId = request.scheduleId();

        String queueKey = queueServiceV2.getQueueKey(scheduleId);
        redisTemplate.opsForZSet().remove(queueKey, userId.toString());
        clearUserToken(userId, scheduleId);

        tokenService.issueToken(userId, scheduleId);

        log.info("Debug READY 시나리오 구성 완료 - userId={}, scheduleId={}", userId, scheduleId);

        return ResponseEntity.ok(ApiResponse.success(
                QueueDtoV2.DebugScenarioResponse.of(
                        "READY 시나리오 구성 완료",
                        scheduleId,
                        userId,
                        0L,
                        0L
                )
        ));
    }

    /**
     * 로그인 사용자 기준 대기열/토큰 초기화
     */
    @PostMapping("/reset/me")
    public ResponseEntity<ApiResponse<QueueDtoV2.DebugScenarioResponse>> resetMyScenario(
            @Valid @RequestBody QueueDtoV2.DebugResetScenarioRequest request) {
        Long userId = getAuthenticatedUserId();
        Long scheduleId = request.scheduleId();

        String queueKey = queueServiceV2.getQueueKey(scheduleId);
        redisTemplate.opsForZSet().remove(queueKey, userId.toString());
        clearUserToken(userId, scheduleId);

        log.info("Debug 내 시나리오 초기화 완료 - userId={}, scheduleId={}", userId, scheduleId);

        return ResponseEntity.ok(ApiResponse.success(
                QueueDtoV2.DebugScenarioResponse.of(
                        "내 시나리오 초기화 완료",
                        scheduleId,
                        userId,
                        null,
                        null
                )
        ));
    }

    /**
     * 해당 스케줄의 전체 대기열 초기화
     */
    @PostMapping("/reset/all")
    public ResponseEntity<ApiResponse<QueueDtoV2.DebugScenarioResponse>> resetAllScenario(
            @Valid @RequestBody QueueDtoV2.DebugResetScenarioRequest request) {
        Long userId = getAuthenticatedUserId();
        Long scheduleId = request.scheduleId();

        String queueKey = queueServiceV2.getQueueKey(scheduleId);
        String activeSchedulesKey = queueServiceV2.getActiveSchedulesKey();
        Long totalWaiting = redisTemplate.opsForZSet().zCard(queueKey);

        redisTemplate.delete(queueKey);
        redisTemplate.opsForSet().remove(activeSchedulesKey, String.valueOf(scheduleId));
        clearUserToken(userId, scheduleId);

        log.info("Debug 전체 시나리오 초기화 완료 - userId={}, scheduleId={}, removedCount={}",
                userId, scheduleId, totalWaiting);

        return ResponseEntity.ok(ApiResponse.success(
                QueueDtoV2.DebugScenarioResponse.of(
                        "전체 시나리오 초기화 완료",
                        scheduleId,
                        userId,
                        null,
                        totalWaiting
                )
        ));
    }

    /**
     * 하위호환: 기존 reset 엔드포인트는 reset/me 와 동일하게 동작
     */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<QueueDtoV2.DebugScenarioResponse>> resetScenario(
            @Valid @RequestBody QueueDtoV2.DebugResetScenarioRequest request) {
        return resetMyScenario(request);
    }

    private void clearUserToken(Long userId, Long scheduleId) {
        String existingToken = tokenService.findTokenByUser(userId, scheduleId);
        if (existingToken != null) {
            tokenService.deleteToken(existingToken);
        }
        redisTemplate.delete(USER_TOKEN_PREFIX + userId + ":" + scheduleId);
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
