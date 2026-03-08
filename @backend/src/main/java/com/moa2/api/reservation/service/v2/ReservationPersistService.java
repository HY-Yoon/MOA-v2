package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.domain.entity.Payment;
import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.entity.ReservationSeat;
import com.moa2.api.reservation.domain.repository.PaymentRepository;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.reservation.domain.repository.ReservationSeatRepository;
import com.moa2.api.reservation.dto.ReservationDtoV2;
import com.moa2.api.show.domain.entity.ScheduleSeat;
import com.moa2.api.show.domain.entity.Show;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.repository.ScheduleSeatRepository;
import com.moa2.api.show.domain.repository.ShowScheduleRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * DB 트랜잭션 전담 서비스 (Spring AOP 프록시 정상 작동용)
 * [2단계] 주문 생성 시 "실제 DB 쓰기 작업"만 담당.
 * - ScheduleSeat 상태는 변경하지 않음 (Redis TTL이 선점 관리)
 * - 결제 완료 시에만 ScheduleSeat → SOLD 처리
 */
@Slf4j
@Profile("v2")
@Service
@RequiredArgsConstructor
public class ReservationPersistService {

    private final UserRepository userRepository;
    private final ShowScheduleRepository showScheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PaymentRepository paymentRepository;

    @Value("${app.payment.success-url}")
    private String successUrl;

    @Value("${app.payment.fail-url}")
    private String failUrl;

    @Value("${queue.token.ttl-minutes:5}")
    private int lockTtlMinutes;

    /**
     * [2단계] 주문 생성 — Reservation + ReservationSeat + Payment(PENDING) 저장
     * ScheduleSeat 상태는 건드리지 않음 (Redis TTL이 선점 관리)
     */
    @Transactional
    public ReservationDtoV2.CreateOrderResponse createOrder(
            Long userId, Long scheduleId, List<Long> scheduleSeatIds,
            String bookerName, String bookerPhone, String bookerEmail) {

        // 사용자 및 스케줄 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
        ShowSchedule schedule = showScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다"));

        // 좌석 조회
        List<ScheduleSeat> seats = scheduleSeatIds.stream()
                .map(id -> scheduleSeatRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다: " + id)))
                .toList();

        // 총 금액 계산
        int totalAmount = seats.stream()
                .mapToInt(seat -> seat.getGrade().getPrice())
                .sum();

        String reservationNumber = generateReservationNumber();
        LocalDateTime paymentDeadline = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .plusMinutes(lockTtlMinutes);

        // 1. ScheduleSeat를 LOCKED 상태로 변경 (결제 검증용)
        for (ScheduleSeat seat : seats) {
            seat.lock(userId, paymentDeadline);
            scheduleSeatRepository.save(seat);
        }

        // 2. Reservation 생성 (PENDING)
        Reservation reservation = Reservation.builder()
                .user(user)
                .showSchedule(schedule)
                .reservationNumber(reservationNumber)
                .totalAmount(totalAmount)
                .seatCount(seats.size())
                .bookerName(bookerName)
                .bookerPhone(bookerPhone)
                .bookerEmail(bookerEmail)
                .build();
        reservationRepository.save(reservation);

        // 3. ReservationSeat 연결
        for (ScheduleSeat scheduleSeat : seats) {
            ReservationSeat reservationSeat = ReservationSeat.builder()
                    .reservation(reservation)
                    .seat(scheduleSeat.getSeat())
                    .scheduleSeatId(scheduleSeat.getId())
                    .price(scheduleSeat.getGrade().getPrice())
                    .build();
            reservationSeatRepository.save(reservationSeat);
        }

        // 4. Payment(PENDING) 생성
        String orderId = generateOrderId();
        Payment payment = Payment.builder()
                .reservation(reservation)
                .orderId(orderId)
                .amount(totalAmount)
                .build();
        paymentRepository.save(payment);

        // 5. 주문명 생성
        Show show = schedule.getShow();
        String orderName = createOrderName(show.getTitle(), seats.size());

        log.info("V2 주문 생성 완료 - reservationId: {}, orderId: {}, totalAmount: {}",
                reservation.getId(), orderId, totalAmount);

        return ReservationDtoV2.CreateOrderResponse.builder()
                .orderId(orderId)
                .totalAmount(totalAmount)
                .orderName(orderName)
                .paymentDeadline(paymentDeadline)
                .successUrl(successUrl + (successUrl.contains("?") ? "&" : "?") + "orderId=" + orderId)
                .failUrl(failUrl + (failUrl.contains("?") ? "&" : "?") + "orderId=" + orderId)
                .booker(new ReservationDtoV2.CreateOrderResponse.BookerInfo(
                        bookerName, bookerEmail, bookerPhone))
                .build();
    }

    private String generateReservationNumber() {
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "RES-" + date + "-" + random;
    }

    private String generateOrderId() {
        return "MOA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String createOrderName(String showTitle, int seatCount) {
        String truncatedTitle = showTitle.length() > 30
                ? showTitle.substring(0, 30) + "..."
                : showTitle;
        return truncatedTitle + " - " + seatCount + "좌석";
    }
}
