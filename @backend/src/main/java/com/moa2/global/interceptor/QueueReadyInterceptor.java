package com.moa2.global.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa2.domain.queue.entity.Queue;
import com.moa2.domain.queue.repository.QueueRepository;
import com.moa2.domain.user.entity.User;
import com.moa2.domain.user.repository.UserRepository;
import com.moa2.global.dto.ApiResponse;
import com.moa2.global.model.QueueStatus;
import com.moa2.global.model.SocialProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 좌석 배치도 조회 전, 대기열 READY 상태를 검증하는 인터셉터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueReadyInterceptor implements HandlerInterceptor {

    private static final Pattern SCHEDULE_SEATS_URI_PATTERN =
            Pattern.compile("^/api/v1/schedules/(\\d+)/seats$");

    private final QueueRepository queueRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        Matcher matcher = SCHEDULE_SEATS_URI_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            return true;
        }

        Long scheduleId = Long.parseLong(matcher.group(1));

        // 1) 인증 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            writeError(response, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
            return false;
        }

        // 2) JWT에서 email + provider 기반으로 사용자 조회
        String email = (String) authentication.getPrincipal();
        String provider = (String) authentication.getCredentials();
        if (provider == null || provider.isBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "인증 정보(provider)가 없습니다. 다시 로그인해주세요.");
            return false;
        }

        SocialProvider socialProvider;
        try {
            socialProvider = SocialProvider.valueOf(provider);
        } catch (IllegalArgumentException e) {
            writeError(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 provider 입니다. 다시 로그인해주세요.");
            return false;
        }

        User user = userRepository.findByEmailAndSocialProvider(email, socialProvider)
                .orElse(null);
        if (user == null) {
            writeError(response, HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다. 다시 로그인해주세요.");
            return false;
        }

        // 3) 대기열 상태 확인 (READY + 만료되지 않아야 함)
        Optional<Queue> queueOpt = queueRepository.findByUserIdAndScheduleId(user.getId(), scheduleId);
        if (queueOpt.isEmpty()) {
            writeError(response, HttpStatus.FORBIDDEN, "대기열을 통과한 사용자만 접근할 수 있습니다. 먼저 대기열에 진입해주세요.");
            return false;
        }

        Queue queue = queueOpt.get();
        if (queue.getStatus() != QueueStatus.READY) {
            writeError(response, HttpStatus.FORBIDDEN, "대기열 READY 상태가 아닙니다. 현재 상태: " + queue.getStatus());
            return false;
        }

        if (!queue.isActiveSessionValid()) {
            // READY 만료 → EXPIRED 처리
            queue.expire();
            queueRepository.save(queue);
            writeError(response, HttpStatus.FORBIDDEN, "대기열 세션이 만료되었습니다. 다시 대기열에 진입해주세요.");
            return false;
        }

        return true;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }
}

