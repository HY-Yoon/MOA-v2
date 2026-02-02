package com.moa2.api.reservation.service;

import com.moa2.api.reservation.dto.ReservationDto;
import com.moa2.api.reservation.domain.entity.Payment;
import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.entity.ReservationSeat;
import com.moa2.api.reservation.domain.repository.PaymentRepository;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.reservation.domain.repository.ReservationSeatRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa2.api.reservation.domain.specification.ReservationSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import com.moa2.api.reservation.dto.ReservationSearchCondition;

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
        public PageResponse<ReservationDto.ListResponse> getMyReservations(String email,
                        ReservationSearchCondition condition, Pageable pageable) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

                Specification<Reservation> spec = Specification.where(ReservationSpecification.equalUser(user));

                if (condition.status() != null) {
                        spec = spec.and(ReservationSpecification.equalStatus(condition.status()));
                }

                if (condition.paymentStatus() != null) {
                        spec = spec.and(ReservationSpecification.equalPaymentStatus(condition.paymentStatus()));
                }

                if (condition.startDate() != null || condition.endDate() != null) {
                        spec = spec.and(ReservationSpecification.betweenDate(condition.dateType(),
                                        condition.startDate(), condition.endDate()));
                }

                // Specification + Pageable 조회 (N+1 방지는 Repository의 @EntityGraph로 처리됨)
                Page<Reservation> reservations = reservationRepository.findAll(spec, pageable);

                // Entity -> DTO 변환 (Factory Method 사용)
                Page<ReservationDto.ListResponse> responseList = reservations.map(ReservationDto.ListResponse::from);

                return PageResponse.of(responseList);
        }

        /**
         * 예매 상세 조회
         */
        @Transactional(readOnly = true)
        public ReservationDto.DetailResponse getReservationDetail(String email, Long reservationId) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

                // 본인의 예매만 조회 가능 (N+1 방지: Fetch Join)
                Reservation reservation = reservationRepository.findWithDetailsByIdAndUser(reservationId, user)
                                .orElseThrow(() -> new IllegalArgumentException("예매 정보를 찾을 수 없습니다: " + reservationId));

                // 좌석 정보 조회 (Lazy Loading이지만 Transaction 안이라 안전, BatchSize로 최적화 권장)
                List<ReservationSeat> reservationSeats = reservationSeatRepository
                                .findByReservationWithSeat(reservation);

                // Entity -> DTO 변환 (Factory Method 사용)
                return ReservationDto.DetailResponse.from(reservation, reservationSeats);
        }

        /**
         * 예매 취소
         */
        @Transactional
        public ReservationDto.CancelResponse cancelReservation(String email, Long reservationId) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

                // 본인의 예매만 취소 가능 (단건 조회라 일반 메서드 사용해도 무방하지만 일관성 위해 withDetails 권장)
                Reservation reservation = reservationRepository.findByIdAndUser(reservationId, user)
                                .orElseThrow(() -> new IllegalArgumentException("예매 정보를 찾을 수 없습니다: " + reservationId));

                // 비즈니스 로직: 취소 가능 여부 검증 (Entity에게 위임)
                reservation.validateCancellable();

                // 예매 취소 처리
                reservation.cancel();
                // reservationRepository.save(reservation); // Dirty Checking

                // 결제 취소 처리
                Payment payment = paymentRepository.findByReservation(reservation).orElse(null);
                if (payment != null) {
                        payment.cancel("사용자 요청");
                        // paymentRepository.save(payment); // Dirty Checking
                }

                log.info("예매 취소 완료: {} (예매번호: {})", email, reservation.getReservationNumber());

                return ReservationDto.CancelResponse.builder()
                                .reservationId(reservation.getId())
                                .reservationNumber(reservation.getReservationNumber())
                                .message("예매가 취소되었습니다.")
                                .build();
        }
}
