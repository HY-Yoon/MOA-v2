package com.moa2.api.reservation.service;

import com.moa2.api.reservation.dto.ReservationDto;
import com.moa2.api.reservation.domain.entity.Payment;
import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.entity.ReservationSeat;
import com.moa2.api.reservation.domain.repository.PaymentRepository;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.reservation.domain.repository.ReservationSeatRepository;
import com.moa2.api.show.domain.entity.Show;
import com.moa2.api.show.domain.entity.ShowSchedule;
import com.moa2.api.show.domain.entity.Venue;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.PageResponse;
import com.moa2.global.model.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 예매 관련 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    /**
     * 내 예매 내역 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<ReservationDto.ListResponse> getMyReservations(String email, String status, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        Page<Reservation> reservations;
        
        if (status != null && !status.isEmpty()) {
            // 특정 상태만 조회
            ReservationStatus reservationStatus = ReservationStatus.valueOf(status);
            reservations = reservationRepository.findByUserAndStatus(user, reservationStatus, pageable);
        } else {
            // 모든 예매 조회
            reservations = reservationRepository.findByUser(user, pageable);
        }
        
        Page<ReservationDto.ListResponse> responseList = reservations.map(reservation -> {
            ShowSchedule schedule = reservation.getShowSchedule();
            Show show = schedule.getShow();
            Venue venue = show.getVenue();
            
            // 결제 정보 조회
            Payment payment = paymentRepository.findByReservation(reservation).orElse(null);
            
            // 취소 가능 여부 판단 (CONFIRMED 상태이고, 공연일 2일 전까지)
            boolean canCancel = reservation.getStatus() == ReservationStatus.CONFIRMED 
                    && schedule.getShowDate().minusDays(2).atStartOfDay().isAfter(LocalDateTime.now());
            
            return ReservationDto.ListResponse.builder()
                    .reservationId(reservation.getId())
                    .reservationNumber(reservation.getReservationNumber())
                    .reservationDate(reservation.getCreatedAt())
                    .reservationStatus(reservation.getStatus().name())
                    .paymentStatus(payment != null ? payment.getStatus().name() : null)
                    .show(ReservationDto.ShowInfo.builder()
                            .showId(show.getId())
                            .title(show.getTitle())
                            .posterUrl(show.getPosterUrl())
                            .genre(show.getGenre().name())
                            .build())
                    .schedule(ReservationDto.ScheduleInfo.builder()
                            .scheduleId(schedule.getId())
                            .showDate(schedule.getShowDate())
                            .showTime(schedule.getShowTime())
                            .location(ReservationDto.LocationInfo.builder()
                                    .region(venue.getRegion().name())
                                    .venue(venue.getName())
                                    .hallName(venue.getHallName())
                                    .build())
                            .build())
                    .seatCount(reservation.getSeatCount())
                    .totalAmount(reservation.getTotalAmount())
                    .canCancel(canCancel)
                    .build();
        });
        
        return PageResponse.of(responseList);
    }

    /**
     * 예매 상세 조회
     */
    @Transactional(readOnly = true)
    public ReservationDto.DetailResponse getReservationDetail(String email, Long reservationId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        // 본인의 예매만 조회 가능
        Reservation reservation = reservationRepository.findByIdAndUser(reservationId, user)
                .orElseThrow(() -> new IllegalArgumentException("예매 정보를 찾을 수 없습니다: " + reservationId));
        
        ShowSchedule schedule = reservation.getShowSchedule();
        Show show = schedule.getShow();
        Venue venue = show.getVenue();
        
        // 좌석 정보 조회
        List<ReservationSeat> reservationSeats = reservationSeatRepository.findByReservationWithSeat(reservation);
        List<ReservationDto.SeatInfo> seatInfos = reservationSeats.stream()
                .map(rs -> ReservationDto.SeatInfo.builder()
                        .seatId(rs.getSeat().getId())
                        .section(rs.getSeat().getSection().getName())
                        .row(rs.getSeat().getSeatRow())
                        .number(rs.getSeat().getSeatNumber())
                        .price(rs.getPrice())
                        .build())
                .collect(Collectors.toList());
        
        // 결제 정보 조회
        Payment payment = paymentRepository.findByReservation(reservation).orElse(null);
        
        // 취소 가능 여부 판단
        boolean canCancel = reservation.getStatus() == ReservationStatus.CONFIRMED 
                && schedule.getShowDate().minusDays(2).atStartOfDay().isAfter(LocalDateTime.now());
        
        return ReservationDto.DetailResponse.builder()
                .reservationId(reservation.getId())
                .reservationNumber(reservation.getReservationNumber())
                .reservationDate(reservation.getCreatedAt())
                .reservationStatus(reservation.getStatus().name())
                .show(ReservationDto.ShowInfo.builder()
                        .showId(show.getId())
                        .title(show.getTitle())
                        .posterUrl(show.getPosterUrl())
                        .genre(show.getGenre().name())
                        .runningTime(show.getRunningTime())
                        .cast(show.getCast())
                        .build())
                .schedule(ReservationDto.ScheduleInfo.builder()
                        .scheduleId(schedule.getId())
                        .showDate(schedule.getShowDate())
                        .showTime(schedule.getShowTime())
                        .location(ReservationDto.LocationInfo.builder()
                                .region(venue.getRegion().name())
                                .venue(venue.getName())
                                .hallName(venue.getHallName())
                                .address(venue.getAddress())
                                .build())
                        .build())
                .seats(seatInfos)
                .seatCount(reservation.getSeatCount())
                .booker(ReservationDto.BookerInfo.builder()
                        .name(reservation.getBookerName())
                        .phone(reservation.getBookerPhone())
                        .email(reservation.getBookerEmail())
                        .build())
                .payment(payment != null ? ReservationDto.PaymentInfo.builder()
                        .orderId(payment.getOrderId())
                        .paymentKey(payment.getPaymentKey())
                        .totalAmount(payment.getAmount())
                        .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                        .paymentStatus(payment.getStatus().name())
                        .paidAt(payment.getApprovedAt())
                        .build() : null)
                .canCancel(canCancel)
                .cancelledAt(reservation.getCancelledAt())
                .build();
    }

    /**
     * 예매 취소
     */
    @Transactional
    public ReservationDto.CancelResponse cancelReservation(String email, Long reservationId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        // 본인의 예매만 취소 가능
        Reservation reservation = reservationRepository.findByIdAndUser(reservationId, user)
                .orElseThrow(() -> new IllegalArgumentException("예매 정보를 찾을 수 없습니다: " + reservationId));
        
        // 이미 취소된 예매인지 확인
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예매입니다.");
        }
        
        // PENDING 상태가 아닌 경우 체크
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("확정된 예매만 취소할 수 있습니다.");
        }
        
        // 취소 가능 기한 체크 (공연일 2일 전까지)
        ShowSchedule schedule = reservation.getShowSchedule();
        LocalDateTime cancelDeadline = schedule.getShowDate().minusDays(2).atStartOfDay();
        
        if (LocalDateTime.now().isAfter(cancelDeadline)) {
            throw new IllegalStateException("취소 가능 기한이 지났습니다. (공연일 2일 전까지 취소 가능)");
        }
        
        // 예매 취소 처리
        reservation.cancel();
        reservationRepository.save(reservation);
        
        // 결제 취소 처리
        Payment payment = paymentRepository.findByReservation(reservation).orElse(null);
        if (payment != null) {
            payment.cancel("사용자 요청");
            paymentRepository.save(payment);
        }
        
        log.info("예매 취소 완료: {} (예매번호: {})", email, reservation.getReservationNumber());
        
        return ReservationDto.CancelResponse.builder()
                .reservationId(reservation.getId())
                .reservationNumber(reservation.getReservationNumber())
                .message("예매가 취소되었습니다.")
                .build();
    }
}
