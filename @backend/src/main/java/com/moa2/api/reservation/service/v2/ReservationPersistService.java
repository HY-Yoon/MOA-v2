package com.moa2.api.reservation.service.v2;

import com.moa2.api.reservation.domain.entity.Payment;
import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.entity.ReservationSeat;
import com.moa2.api.reservation.domain.repository.PaymentRepository;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.reservation.domain.repository.ReservationSeatRepository;
import com.moa2.api.reservation.dto.ReservationDtoV2;
import com.moa2.api.show.domain.entity.ScheduleSeat;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.repository.ScheduleSeatRepository;
import com.moa2.api.show.domain.repository.ShowScheduleRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * DB 트랜잭션 전담 서비스 (Spring AOP 프록시 정상 작동용)
 * 분산 락을 획득한 이후 "실제 DB 쓰기 작업"만 담당하여 커넥션 점유를 최소화.
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

    @Transactional
    public ReservationDtoV2.ReserveResponse saveReservationData(
            Long userId, Long scheduleId, List<ScheduleSeat> reservedSeats,
            int totalAmount, int lockTtlMinutes) {

        // 사용자 및 스케줄 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
        ShowSchedule schedule = showScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다"));

        String reservationNumber = generateReservationNumber();
        LocalDateTime paymentDeadline = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .plusMinutes(lockTtlMinutes);

        // 1. 예약 생성
        Reservation reservation = Reservation.builder()
                .user(user)
                .showSchedule(schedule)
                .reservationNumber(reservationNumber)
                .totalAmount(totalAmount)
                .seatCount(reservedSeats.size())
                .bookerName(user.getName())
                .bookerPhone(user.getPhone())
                .bookerEmail(user.getEmail())
                .build();
        reservationRepository.save(reservation);

        // 2. 좌석 상태 변경 + 예약 좌석 연결
        for (ScheduleSeat scheduleSeat : reservedSeats) {
            ScheduleSeat managedSeat = scheduleSeatRepository.findById(scheduleSeat.getId())
                    .orElseThrow(() -> new IllegalArgumentException("좌석을 찾을 수 없습니다"));

            LocalDateTime lockedUntil = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .plusMinutes(lockTtlMinutes);
            managedSeat.lock(userId, lockedUntil);
            managedSeat.reserve();
            
            // 더티 체킹에만 의존하지 않고 명시적 저장 추가 (상태 변경 확실히 반영)
            scheduleSeatRepository.save(managedSeat);

            ReservationSeat reservationSeat = ReservationSeat.builder()
                    .reservation(reservation)
                    .seat(managedSeat.getSeat())
                    .scheduleSeatId(managedSeat.getId())
                    .price(managedSeat.getGrade().getPrice())
                    .build();
            reservationSeatRepository.save(reservationSeat);
        }

        // 3. Payment(PENDING) 생성
        String orderId = generateOrderId();
        Payment payment = Payment.builder()
                .reservation(reservation)
                .orderId(orderId)
                .amount(totalAmount)
                .build();
        paymentRepository.save(payment);

        log.info("V2 예매 완료 - reservationId: {}, reservationNumber: {}, orderId: {}",
                reservation.getId(), reservationNumber, orderId);

        return ReservationDtoV2.ReserveResponse.success(
                reservation.getId(),
                reservationNumber,
                orderId,
                reservedSeats.size(),
                totalAmount,
                paymentDeadline);
    }

    private String generateReservationNumber() {
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "RES-" + date + "-" + random;
    }

    private String generateOrderId() {
        return "MOA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
