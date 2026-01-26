package com.moa2.api.payment.service;

import com.moa2.api.payment.client.TossPaymentClient;
import com.moa2.api.payment.client.TossPaymentResponse;
import com.moa2.api.payment.dto.PaymentDto;
import com.moa2.api.payment.exception.PaymentException;
import com.moa2.api.payment.exception.TossPaymentException;
import com.moa2.api.reservation.domain.entity.Payment;
import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.entity.ReservationSeat;
import com.moa2.api.reservation.domain.repository.PaymentRepository;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.reservation.domain.repository.ReservationSeatRepository;
import com.moa2.api.show.domain.entity.ScheduleSeat;
import com.moa2.api.show.domain.entity.Show;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.repository.ScheduleSeatRepository;
import com.moa2.api.show.domain.repository.ShowScheduleRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.model.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 결제 서비스
 * 결제 요청/승인/실패 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

        private final PaymentRepository paymentRepository;
        private final ReservationRepository reservationRepository;
        private final ReservationSeatRepository reservationSeatRepository;
        private final ScheduleSeatRepository scheduleSeatRepository;
        private final ShowScheduleRepository showScheduleRepository;
        private final UserRepository userRepository;
        private final TossPaymentClient tossPaymentClient;

        @Value("${app.payment.success-url}")
        private String successUrl;

        @Value("${app.payment.fail-url}")
        private String failUrl;

        /**
         * Step 5-1: 결제 요청
         * - 좌석 선점 상태 검증
         * - Reservation, Payment 생성 (PENDING)
         * - 토스 위젯용 데이터 반환
         */
        @Transactional
        public PaymentDto.RequestResponse requestPayment(PaymentDto.Request request, Long userId) {
                log.info("결제 요청 시작: userId={}, scheduleId={}, seatIds={}",
                                userId, request.scheduleId(), request.seatIds());

                // 1. 사용자 조회
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> PaymentException.notFound("사용자를 찾을 수 없습니다."));

                // 2. 스케줄 조회
                ShowSchedule schedule = showScheduleRepository.findById(request.scheduleId())
                                .orElseThrow(() -> PaymentException.notFound("공연 스케줄을 찾을 수 없습니다."));

                // 3. 좌석 조회 및 선점 상태 검증 (비관적 락)
                List<ScheduleSeat> scheduleSeats = scheduleSeatRepository
                                .findByScheduleIdAndSeatIdInForUpdate(request.scheduleId(), request.seatIds());

                if (scheduleSeats.size() != request.seatIds().size()) {
                        throw PaymentException.notFound("일부 좌석을 찾을 수 없습니다.");
                }

                // 4. 좌석 선점 상태 검증 (본인이 LOCKED 상태인지)
                for (ScheduleSeat seat : scheduleSeats) {
                        if (!seat.isLockedBy(userId)) {
                                throw PaymentException.seatNotLocked(
                                                "좌석이 선점되지 않았거나 다른 사용자가 선점했습니다. 좌석: " + seat.getSeat().getSeatNumber());
                        }
                        if (seat.isLockExpired()) {
                                throw PaymentException.lockExpired(
                                                "좌석 선점 시간이 만료되었습니다. 좌석: " + seat.getSeat().getSeatNumber());
                        }
                }

                // 5. 총 금액 계산
                int totalAmount = scheduleSeats.stream()
                                .mapToInt(ss -> ss.getGrade().getPrice())
                                .sum();

                // 6. orderId 생성 (UUID)
                String orderId = generateOrderId();

                // 7. 예매번호 생성
                String reservationNumber = generateReservationNumber();

                // 8. Reservation 생성 (PENDING)
                Reservation reservation = Reservation.builder()
                                .user(user)
                                .showSchedule(schedule)
                                .reservationNumber(reservationNumber)
                                .totalAmount(totalAmount)
                                .seatCount(scheduleSeats.size())
                                .bookerName(request.bookerName())
                                .bookerPhone(request.bookerPhone())
                                .bookerEmail(request.bookerEmail())
                                .build();
                reservationRepository.save(reservation);

                // 9. ReservationSeat 생성 (결제 당시 가격 저장)
                for (ScheduleSeat scheduleSeat : scheduleSeats) {
                        ReservationSeat reservationSeat = ReservationSeat.builder()
                                        .reservation(reservation)
                                        .seat(scheduleSeat.getSeat())
                                        .price(scheduleSeat.getGrade().getPrice())
                                        .build();
                        reservationSeatRepository.save(reservationSeat);
                }

                // 10. Payment 생성 (PENDING)
                Payment payment = Payment.builder()
                                .reservation(reservation)
                                .orderId(orderId)
                                .amount(totalAmount)
                                .build();
                paymentRepository.save(payment);

                // 11. 주문명 생성
                Show show = schedule.getShow();
                String orderName = createOrderName(show.getTitle(), scheduleSeats.size());

                log.info("결제 요청 완료: orderId={}, reservationNumber={}, amount={}",
                                orderId, reservationNumber, totalAmount);

                return new PaymentDto.RequestResponse(
                                orderId,
                                totalAmount,
                                orderName,
                                request.bookerName(),
                                request.bookerEmail(),
                                request.bookerPhone(),
                                successUrl + "?orderId=" + orderId,
                                failUrl + "?orderId=" + orderId);
        }

        /**
         * Step 5-2: 결제 승인
         * - 3단 검증 (좌석 상태, 금액, 선점 시간)
         * - 검증 실패 시 토스 취소 API 호출 후 예외 발생
         * - 검증 성공 시 토스 승인 API 호출
         * - Payment → COMPLETED, ScheduleSeat → SOLD, Reservation → CONFIRMED
         */
        @Transactional
        public PaymentDto.SuccessResponse confirmPayment(PaymentDto.ConfirmRequest request, Long userId) {
                log.info("결제 승인 시작: orderId={}, paymentKey={}, amount={}, userId={}",
                                request.orderId(), request.paymentKey(), request.amount(), userId);

                // 1. Payment 조회
                Payment payment = paymentRepository.findByOrderIdWithReservation(request.orderId())
                                .orElseThrow(() -> PaymentException.notFound("결제 정보를 찾을 수 없습니다."));

                Reservation reservation = payment.getReservation();

                // 2. 본인 예매인지 확인
                if (!reservation.getUser().getId().equals(userId)) {
                        throw PaymentException.unauthorized("본인의 결제만 승인할 수 있습니다.");
                }

                // 3. 예약에 포함된 좌석 조회 (ReservationSeat -> Seat ID 추출)
                List<ReservationSeat> reservationSeats = reservationSeatRepository
                                .findByReservationWithSeat(reservation);

                List<Long> seatIds = reservationSeats.stream()
                                .map(rs -> rs.getSeat().getId())
                                .toList();

                // 4. ScheduleSeat 비관적 락으로 조회
                List<ScheduleSeat> scheduleSeats = scheduleSeatRepository
                                .findByScheduleIdAndSeatIdInForUpdate(
                                                reservation.getShowSchedule().getId(), seatIds);

                // 5. ===== 3단 검증 =====
                try {
                        // 검증 1 & 2: 좌석이 LOCKED 상태이고 본인 선점인지
                        for (ScheduleSeat seat : scheduleSeats) {
                                if (!seat.isLockedBy(userId)) {
                                        throw PaymentException.seatNotLocked(
                                                        "좌석 선점 권한이 없습니다. 좌석: " + seat.getSeat().getSeatNumber());
                                }
                                // 검증 3: 선점 시간 만료 여부
                                if (seat.isLockExpired()) {
                                        throw PaymentException.lockExpired(
                                                        "좌석 선점 시간이 만료되었습니다. 좌석: " + seat.getSeat().getSeatNumber());
                                }
                        }

                        // 검증 4: 금액 일치 여부
                        if (!payment.getAmount().equals(request.amount())) {
                                throw PaymentException.amountMismatch(
                                                "결제 금액이 일치하지 않습니다. 예상: " + payment.getAmount() + ", 실제: "
                                                                + request.amount());
                        }

                } catch (PaymentException e) {
                        // 검증 실패 시 토스 결제 취소 (이미 토스 인증은 통과된 상태)
                        log.warn("3단 검증 실패, 토스 결제 취소 진행: orderId={}, reason={}",
                                        request.orderId(), e.getMessage());
                        cancelTossPaymentSafely(request.paymentKey(), e.getMessage());
                        throw e;
                }

                // 6. 토스 결제 승인 API 호출
                TossPaymentResponse tossResponse;
                try {
                        tossResponse = tossPaymentClient.confirmPayment(
                                        request.paymentKey(), request.orderId(), request.amount());
                } catch (TossPaymentException e) {
                        log.error("토스 결제 승인 실패: orderId={}, code={}, message={}",
                                        request.orderId(), e.getCode(), e.getMessage());
                        throw PaymentException.invalidState("결제 승인에 실패했습니다: " + e.getMessage());
                }

                // 7. 상태 확정
                // Payment → COMPLETED
                PaymentMethod paymentMethod = parsePaymentMethod(tossResponse.method());
                payment.approve(request.paymentKey(), paymentMethod);

                // Reservation → CONFIRMED
                reservation.confirm();

                // ScheduleSeat → SOLD
                scheduleSeats.forEach(ScheduleSeat::markAsSold);

                log.info("결제 승인 완료: orderId={}, reservationNumber={}",
                                request.orderId(), reservation.getReservationNumber());

                return new PaymentDto.SuccessResponse(
                                reservation.getId(),
                                reservation.getReservationNumber(),
                                payment.getOrderId(),
                                payment.getPaymentKey(),
                                payment.getAmount(),
                                tossResponse.method(),
                                tossResponse.orderName(),
                                payment.getApprovedAt());
        }

        /**
         * Step 5-3: 결제 실패
         * - Payment → FAILED
         * - Reservation → CANCELLED
         * - ScheduleSeat → AVAILABLE (락 해제)
         */
        @Transactional
        public void failPayment(PaymentDto.FailRequest request) {
                log.info("결제 실패 처리: orderId={}, code={}, message={}",
                                request.orderId(), request.code(), request.message());

                // orderId가 없는 경우 (PAY_PROCESS_CANCELED)
                if (request.orderId() == null || request.orderId().isBlank()) {
                        log.warn("결제 실패 처리 - orderId 없음 (사용자 취소)");
                        return;
                }

                // 1. Payment 조회
                Payment payment = paymentRepository.findByOrderIdWithReservation(request.orderId())
                                .orElse(null);

                if (payment == null) {
                        log.warn("결제 실패 처리 - Payment를 찾을 수 없음: orderId={}", request.orderId());
                        return;
                }

                Reservation reservation = payment.getReservation();

                // 2. Payment 실패 처리
                String failReason = request.code() + ": " + request.message();
                payment.fail(failReason);

                // 3. Reservation 취소
                reservation.cancel();

                // 4. 좌석 락 해제
                List<ReservationSeat> reservationSeats = reservationSeatRepository
                                .findByReservationWithSeat(reservation);

                List<Long> seatIds = reservationSeats.stream()
                                .map(rs -> rs.getSeat().getId())
                                .toList();

                List<ScheduleSeat> scheduleSeats = scheduleSeatRepository
                                .findByScheduleIdAndSeatIdInForUpdate(
                                                reservation.getShowSchedule().getId(), seatIds);

                scheduleSeats.forEach(ScheduleSeat::releaseLock);

                log.info("결제 실패 처리 완료: orderId={}, 좌석 {} 개 락 해제",
                                request.orderId(), scheduleSeats.size());
        }

        /**
         * 토스 결제 취소 (안전하게 - 예외 무시)
         */
        private void cancelTossPaymentSafely(String paymentKey, String reason) {
                try {
                        tossPaymentClient.cancelPayment(paymentKey, reason);
                } catch (Exception e) {
                        log.error("토스 결제 취소 실패 (무시됨): paymentKey={}, error={}", paymentKey, e.getMessage());
                }
        }

        /**
         * orderId 생성 (UUID 기반, 토스 규격에 맞게)
         * 영문 대소문자, 숫자, -, _, = 로 구성된 6~64자
         */
        private String generateOrderId() {
                return "MOA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        }

        /**
         * 예매번호 생성
         */
        private String generateReservationNumber() {
                return "R" + System.currentTimeMillis() +
                                String.format("%04d", (int) (Math.random() * 10000));
        }

        /**
         * 주문명 생성
         */
        private String createOrderName(String showTitle, int seatCount) {
                String truncatedTitle = showTitle.length() > 30
                                ? showTitle.substring(0, 30) + "..."
                                : showTitle;
                return truncatedTitle + " - " + seatCount + "좌석";
        }

        /**
         * 결제 수단 파싱
         */
        private PaymentMethod parsePaymentMethod(String method) {
                if (method == null)
                        return PaymentMethod.ETC;

                return switch (method) {
                        case "카드" -> PaymentMethod.CARD;
                        case "가상계좌" -> PaymentMethod.VIRTUAL_ACCOUNT;
                        case "계좌이체" -> PaymentMethod.TRANSFER;
                        case "휴대폰" -> PaymentMethod.MOBILE;
                        default -> PaymentMethod.ETC;
                };
        }
}
