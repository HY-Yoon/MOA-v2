package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.dto.ReservationDtoV2;
import com.moa2.global.token.TokenInfo;
import com.moa2.global.token.TokenService;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.springframework.context.annotation.Profile;
import java.util.List;

/**
 * V2: 예매 통합 관리 Facade
 * - 토큰 검증 → 좌석 선점 → 토큰 소진 (1단계)
 * - Redis 선점 확인 → 주문 생성 (2단계)
 */
@Slf4j
@Profile("v2")
@Component
@RequiredArgsConstructor
public class ReservationFacade {

    private final TokenService tokenService;
    private final ReservationServiceV2 reservationServiceV2;
    private final ReservationPersistService persistService;
    private final UserRepository userRepository;

    /**
     * [1단계] 좌석 선점 (Redis Only)
     * - 토큰 검증 → 좌석 선점 → 토큰 소진
     * - DB Write 없음, 즉시 200 응답
     */
    public ReservationDtoV2.ReserveSeatResponse reserve(
            String token,
            Long userId,
            ReservationDtoV2.ReserveRequest request) {
        log.info("V2 좌석 선점 Facade 시작 - token: {}, userId: {}", token, userId);

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

        // 4. Redis 선점 (DB Write 없음!)
        ReservationDtoV2.ReserveSeatResponse response = reservationServiceV2.reserveSeats(
                userId,
                request.scheduleId(),
                request.scheduleSeatIds());

        // 5. 토큰 소진 (재사용 방지)
        tokenService.consumeToken(token);

        log.info("V2 좌석 선점 Facade 완료 - {}좌석 선점, 남은시간: {}초",
                response.seatCount(), response.remainingSeconds());

        return response;
    }

    /**
     * [미리보기] 주문 상세 조회 (결제 페이지용)
     */
    public ReservationDtoV2.PreviewResponse getPreviewInfo(
            Long userId,
            Long scheduleId,
            List<Long> seatIds) {
        log.info("V2 주문 미리보기 Facade 시작 - userId: {}, scheduleId: {}", userId, scheduleId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return reservationServiceV2.getPreviewInfo(userId, scheduleId, seatIds, user);
    }

    /**
     * [2단계] 주문 생성 (DB Write)
     * - Redis 선점 유효성 확인 → DB에 Reservation + Payment(PENDING) 저장
     * - orderId를 반환하여 Toss 위젯 초기화
     */
    public ReservationDtoV2.CreateOrderResponse createOrder(
            Long userId,
            ReservationDtoV2.CreateOrderRequest request) {
        log.info("V2 주문 생성 Facade 시작 - userId: {}, scheduleId: {}", userId, request.scheduleId());

        // 1. Redis 선점 유효성 확인 (아직 이 사용자의 선점이 유효한지?)
        reservationServiceV2.validateSeatHold(userId, request.scheduleSeatIds());

        // 2. DB 저장 (Reservation + ReservationSeat + Payment)
        ReservationDtoV2.CreateOrderResponse response = persistService.createOrder(
                userId,
                request.scheduleId(),
                request.scheduleSeatIds(),
                request.bookerName(),
                request.bookerPhone(),
                request.bookerEmail());

        log.info("V2 주문 생성 Facade 완료 - orderId: {}", response.orderId());

        return response;
    }
}
