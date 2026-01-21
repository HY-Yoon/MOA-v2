package com.moa2.api.queue.controller;

import com.moa2.api.queue.dto.QueueEnterRequest;
import com.moa2.api.queue.dto.QueueEnterResponse;
import com.moa2.api.queue.dto.QueueStatusResponse;
import com.moa2.api.queue.service.QueueService;
import com.moa2.domain.user.entity.User;
import com.moa2.domain.user.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.SocialProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final UserRepository userRepository;

    /**
     * 대기열 진입 (줄 서기)
     */
    @Operation(summary = "대기열 진입", description = "공연 예매 대기열에 진입합니다.\n\n" +
            "**인증 방식:** Cookie (accessToken)\n\n" +
            "**처리 과정:**\n" +
            "1. 이미 대기열에 등록되어 있는지 확인\n" +
            "2. 없으면 새로 등록, 있으면 기존 정보 반환\n" +
            "3. 내 앞 대기 인원 수 계산\n\n" +
            "**주의:** 같은 회차에 중복 진입은 불가능.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대기열 진입 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "성공 응답", value = """
                    {
                      "success": true,
                      "data": {
                        "queueId": 5012,
                        "position": 150,
                        "estimatedWaitTime": 300
                      },
                      "message": null
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 - 존재하지 않는 스케줄", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "에러 응답", value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "존재하지 않는 스케줄입니다."
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 - 로그인 필요")
    })
    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<QueueEnterResponse>> enterQueue(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "대기열 진입 요청", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = QueueEnterRequest.class), examples = @ExampleObject(name = "요청 예시", value = """
                    {
                      "scheduleId": 100
                    }
                    """))) @Valid @RequestBody QueueEnterRequest request) {

        try {
            // 현재 로그인한 사용자 ID 가져오기
            Long userId = getAuthenticatedUserId();

            QueueEnterResponse response = queueService.enterQueue(userId, request);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            log.warn("대기열 진입 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 대기 상태 조회 (폴링용)
     */
    @Operation(summary = "대기 상태 조회", description = "내 대기열 상태를 조회합니다. 프론트엔드에서 3초마다 호출하여 상태를 확인합니다.\n\n" +
            "**인증 방식:** Cookie (accessToken)\n\n" +
            "**상태 종류:**\n" +
            "- `WAITING`: 대기 중 (position 포함)\n" +
            "- `READY`: 입장 가능 (activeUntil 포함)\n" +
            "- `EXPIRED`: 시간 초과로 만료됨\n" +
            "- `COMPLETED`: 결제 완료로 대기열 졸업\n\n" +
            "**폴링 주기:** 3초 권장")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 - WAITING 상태", content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(name = "대기 중 (WAITING)", value = """
                            {
                              "success": true,
                              "data": {
                                "status": "WAITING",
                                "position": 42,
                                "estimatedWaitTime": 84
                              },
                              "message": null
                            }
                            """),
                    @ExampleObject(name = "입장 가능 (READY)", value = """
                            {
                              "success": true,
                              "data": {
                                "status": "READY",
                                "position": 0,
                                "activeUntil": "2026-01-20T10:10:00"
                              },
                              "message": null
                            }
                            """),
                    @ExampleObject(name = "만료됨 (EXPIRED)", value = """
                            {
                              "success": true,
                              "data": {
                                "status": "EXPIRED"
                              },
                              "message": null
                            }
                            """)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 - 대기열 정보 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "에러 응답", value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "대기열 정보를 찾을 수 없습니다. 먼저 대기열에 진입해주세요."
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 - 로그인 필요")
    })
    @GetMapping("/tokens/status")
    public ResponseEntity<ApiResponse<QueueStatusResponse>> getQueueStatus(
            @Parameter(description = "스케줄 ID", required = true, example = "100") @RequestParam Long scheduleId) {

        try {
            // 현재 로그인한 사용자 ID 가져오기
            Long userId = getAuthenticatedUserId();

            QueueStatusResponse response = queueService.getQueueStatus(userId, scheduleId);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            log.warn("대기 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * SecurityContext에서 인증된 사용자의 ID를 가져옴
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new IllegalStateException("인증이 필요합니다.");
        }

        String email = (String) authentication.getPrincipal();
        String provider = (String) authentication.getCredentials();
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
