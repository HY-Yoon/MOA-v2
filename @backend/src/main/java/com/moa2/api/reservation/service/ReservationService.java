package com.moa2.api.reservation.service;

import com.moa2.api.reservation.dto.ReservationDto;
import com.moa2.api.reservation.domain.entity.Payment;
import com.moa2.api.reservation.domain.entity.Reservation;
import com.moa2.api.reservation.domain.entity.ReservationSeat;
import com.moa2.api.reservation.domain.repository.PaymentRepository;
import com.moa2.api.reservation.domain.repository.ReservationRepository;
import com.moa2.api.reservation.domain.repository.ReservationSeatRepository;
import com.moa2.api.reservation.service.v2.ReservationServiceV2;
import com.moa2.api.show.domain.entity.ScheduleSeat;
import com.moa2.api.show.domain.repository.ScheduleSeatRepository;
import com.moa2.api.user.domain.entity.User;
import com.moa2.api.user.domain.repository.UserRepository;
import com.moa2.global.dto.PageResponse;
import com.moa2.global.model.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.moa2.api.reservation.domain.specification.ReservationSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
        private final ScheduleSeatRepository scheduleSeatRepository;
        private final ObjectProvider<ReservationServiceV2> reservationServiceV2Provider;

        /**
         * 내 예매 내역 목록 조회
         */
        @Transactional(readOnly = true)
        public PageResponse<ReservationDto.ListResponse> getMyReservations(String email, String provider,
                        ReservationSearchCondition condition, Pageable pageable) {
                User user = findUserByEmailAndProvider(email, provider);

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
        public ReservationDto.DetailResponse getReservationDetail(String email, String provider, Long reservationId) {
                User user = findUserByEmailAndProvider(email, provider);

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
        public ReservationDto.CancelResponse cancelReservation(String email, String provider, Long reservationId) {
                User user = findUserByEmailAndProvider(email, provider);

                // 본인의 예매만 취소 가능 (단건 조회라 일반 메서드 사용해도 무방하지만 일관성 위해 withDetails 권장)
                Reservation reservation = reservationRepository.findByIdAndUser(reservationId, user)
                                .orElseThrow(() -> new IllegalArgumentException("예매 정보를 찾을 수 없습니다: " + reservationId));

                // 비즈니스 로직: 취소 가능 여부 검증 (Entity에게 위임)
                reservation.validateCancellable();

                List<ReservationSeat> reservationSeats = reservationSeatRepository
                                .findByReservationWithSeat(reservation);
                List<Long> scheduleSeatIds = reservationSeats.stream()
                                .map(ReservationSeat::getScheduleSeatId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .sorted()
                                .toList();

                if (!scheduleSeatIds.isEmpty()) {
                        List<ScheduleSeat> scheduleSeats = scheduleSeatRepository
                                        .findByScheduleIdAndScheduleSeatIdsForUpdate(
                                                        reservation.getShowSchedule().getId(), scheduleSeatIds);
                        if (scheduleSeats.size() != scheduleSeatIds.size()) {
                                throw new IllegalStateException("예매에 연결된 좌석을 모두 찾을 수 없습니다.");
                        }
                        for (ScheduleSeat seat : scheduleSeats) {
                                seat.releaseAfterSaleCancelled();
                        }

                        ReservationServiceV2 v2 = reservationServiceV2Provider.getIfAvailable();
                        if (v2 != null) {
                                List<Long> redisReleaseIds = List.copyOf(scheduleSeatIds);
                                TransactionSynchronizationManager.registerSynchronization(
                                                new TransactionSynchronization() {
                                                        @Override
                                                        public void afterCommit() {
                                                                v2.releaseSeatHold(redisReleaseIds);
                                                        }
                                                });
                        }
                }

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

        private User findUserByEmailAndProvider(String email, String provider) {
                if (email == null || email.isBlank() || provider == null || provider.isBlank()) {
                        throw new IllegalArgumentException("인증 정보가 올바르지 않습니다. 다시 로그인해주세요.");
                }

                SocialProvider socialProvider;
                try {
                        socialProvider = SocialProvider.valueOf(provider.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("유효하지 않은 provider 입니다. 다시 로그인해주세요.");
                }

                return userRepository.findByEmailAndSocialProvider(email, socialProvider)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "사용자를 찾을 수 없습니다: " + email + " (" + socialProvider + ")"));
        }
}
