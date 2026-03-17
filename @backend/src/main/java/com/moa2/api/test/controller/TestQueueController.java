package com.moa2.api.test.controller;

import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.repository.ShowScheduleRepository;
import com.moa2.api.test.dto.TestQueueReadyRequest;
import com.moa2.api.test.dto.TestQueueReadyResponse;
import com.moa2.api.queue.queue.entity.Queue;
import com.moa2.api.queue.queue.repository.QueueRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.QueueStatus;
import com.moa2.global.model.SocialProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * local/test 프로필에서만 활성화되는 테스트용 대기열 제어 API.
 * - k6 같은 부하 테스트에서 좌석 API(READY 검증)를 안정적으로 통과하기 위해 READY를 강제 세팅
 */
@Slf4j
@RestController
@RequestMapping("/api/test/queue")
@RequiredArgsConstructor
public class TestQueueController {

    private final UserRepository userRepository;
    private final ShowScheduleRepository showScheduleRepository;
    private final QueueRepository queueRepository;

    @PostMapping("/ready")
    public ResponseEntity<ApiResponse<TestQueueReadyResponse>> forceReady(@Valid @RequestBody TestQueueReadyRequest request) {
        SocialProvider socialProvider;
        try {
            socialProvider = SocialProvider.valueOf(request.getProvider());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("유효하지 않은 provider 입니다: " + request.getProvider()));
        }

        User user = userRepository.findByEmailAndSocialProvider(request.getEmail(), socialProvider)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 먼저 /api/test/auth/login 으로 로그인해주세요."));

        ShowSchedule schedule = showScheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

        int ttlMinutes = (request.getTtlMinutes() == null || request.getTtlMinutes() <= 0) ? 10 : request.getTtlMinutes();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0);
        LocalDateTime activeUntil = now.plusMinutes(ttlMinutes);

        Queue queue = queueRepository.findTopByUserIdAndScheduleIdAndStatusInOrderByCreatedAtDesc(
                        user.getId(),
                        schedule.getId(),
                        List.of(QueueStatus.WAITING, QueueStatus.READY)
                )
                .orElseGet(() -> queueRepository.save(Queue.builder()
                        .user(user)
                        .schedule(schedule)
                        .build()));

        queue.activate(activeUntil);
        Queue saved = queueRepository.save(queue);

        log.info("Test force READY: userId={}, scheduleId={}, queueId={}, activeUntil={}",
                user.getId(), schedule.getId(), saved.getId(), activeUntil);

        return ResponseEntity.ok(ApiResponse.success(TestQueueReadyResponse.builder()
                .queueId(saved.getId())
                .scheduleId(schedule.getId())
                .status(saved.getStatus())
                .activeUntil(saved.getActiveUntil())
                .build()));
    }
}

