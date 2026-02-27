package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.dto.ReservationDtoV2;
import com.moa2.global.token.TokenInfo;
import com.moa2.global.token.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.springframework.context.annotation.Profile;

/**
 * V2: 예매 통합 관리 Facade
 * - 토큰 검증 → 좌석 선점 → 예약 생성 → 토큰 소진
 * - 각 서비스 간 복잡한 흐름을 조율
 */
@Slf4j
@Profile("v2")
@Component
@RequiredArgsConstructor
public class ReservationFacade {

    private final TokenService tokenService;
    private final ReservationServiceV2 reservationServiceV2;

    /**
     * 예매 처리 (전체 흐름 관리)
     *
     * @param token   대기열 통과 토큰
     * @param userId  사용자 ID
     * @param request 예매 요청 정보
     * @return 예매 결과
     */
    public ReservationDtoV2.ReserveResponse reserve(
            String token,
            Long userId,
            ReservationDtoV2.ReserveRequest request) {
        log.info("V2 예매 Facade 시작 - token: {}, userId: {}", token, userId);

        // 1. 토큰 검증
        TokenInfo tokenInfo = tokenService.validateToken(token);

        // 2. 토큰 소유자 확인
        if (!tokenInfo.getUserId().equals(userId)) {
            log.warn("토큰 소유자 불일치 - tokenUserId: {}, requestUserId: {}",
                    tokenInfo.getUserId(), userId);
            throw new IllegalArgumentException("토큰 소유자가 일치하지 않습니다");
        }

        // 3. 토큰 스케줄 확인
        if (!tokenInfo.getScheduleId().equals(request.scheduleId())) {
            log.warn("토큰 스케줄 불일치 - tokenScheduleId: {}, requestScheduleId: {}",
                    tokenInfo.getScheduleId(), request.scheduleId());
            throw new IllegalArgumentException("해당 스케줄에 사용할 수 없는 토큰입니다");
        }

        // 4. 예매 처리 (Redisson 분산 락)
        ReservationDtoV2.ReserveResponse response = reservationServiceV2.reserve(
                userId,
                request.scheduleId(),
                request.scheduleSeatIds());

        // 5. 토큰 소진 (재사용 방지)
        tokenService.consumeToken(token);

        log.info("V2 예매 Facade 완료 - reservationId: {}", response.reservationId());

        return response;
    }
}
